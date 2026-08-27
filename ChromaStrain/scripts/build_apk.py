#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ChromaStrain — SDK-less Android build pipeline.

Builds a signed, installable APK using only:
  * aapt2      (linux binary shipped inside the Apktool jar)
  * android-all.jar (Robolectric's framework jar from Maven Central — acts as android.jar)
  * dx         (dalvik-dx repackage from Maven Central — classes -> classes.dex)
  * uber-apk-signer (signs v1+v2+v3 with an auto-generated debug key)
  * javac      (any JDK 17+; sources compiled with --release 8)

This exists so the game can be built in environments where the official Android
SDK (dl.google.com) is unreachable. On a normal dev machine, prefer the Gradle
project (open ChromaStrain/ in Android Studio) or the GitHub Actions workflow.

Usage:  python3 scripts/build_apk.py [--out dist/ChromaStrain-debug.apk]
"""
import os
import shutil
import struct
import subprocess
import sys
import urllib.request
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)                      # ChromaStrain/
APP_MAIN = os.path.join(ROOT, "app", "src", "main")
BUILD = os.path.join(ROOT, "build_nosdk")
DIST = os.path.join(ROOT, "dist")
TOOLS = os.environ.get("ANDRO_TOOLS", os.path.join(ROOT, "tools"))

MIN_SDK = "24"
TARGET_SDK = "34"
VERSION_CODE = "1"
VERSION_NAME = "1.0.0"

TOOL_URLS = {
    "apktool.jar": "https://github.com/iBotPeaches/Apktool/releases/download/v2.9.3/apktool_2.9.3.jar",
    "android-all.jar": "https://repo1.maven.org/maven2/org/robolectric/android-all/14-robolectric-10818077/android-all-14-robolectric-10818077.jar",
    "dx.jar": "https://repo1.maven.org/maven2/com/jakewharton/android/repackaged/dalvik-dx/14.0.0_r21/dalvik-dx-14.0.0_r21.jar",
    "uber-apk-signer.jar": "https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar",
}

# Assets that must be STORED (uncompressed) so AssetFileDescriptor/SoundPool
# can mmap them, plus resources.arsc which Android 11+ requires uncompressed.
STORED_SUFFIXES = (".wav", ".ogg", ".mp3", ".ttf", ".otf")


def log(msg):
    print("[build] " + msg, flush=True)


def run(cmd, **kw):
    log("$ " + " ".join(cmd))
    env = dict(os.environ)
    # JAVA_TOOL_OPTIONS noise breaks nothing but keep stderr readable.
    p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                       env=env, **kw)
    out = p.stdout.decode("utf-8", "replace")
    if p.returncode != 0:
        print(out)
        raise SystemExit("command failed: " + " ".join(cmd))
    return out


def ensure_tools():
    os.makedirs(TOOLS, exist_ok=True)
    for name, url in TOOL_URLS.items():
        path = os.path.join(TOOLS, name)
        if not os.path.exists(path):
            log("downloading %s" % name)
            urllib.request.urlretrieve(url, path)
    aapt2 = os.path.join(TOOLS, "aapt2_64")
    if not os.path.exists(aapt2):
        log("extracting aapt2 from apktool.jar")
        with zipfile.ZipFile(os.path.join(TOOLS, "apktool.jar")) as z:
            with z.open("prebuilt/linux/aapt2_64") as src, open(aapt2, "wb") as dst:
                shutil.copyfileobj(src, dst)
        os.chmod(aapt2, 0o755)
    return aapt2


def compile_resources(aapt2):
    res_zip = os.path.join(BUILD, "res.zip")
    run([aapt2, "compile", "--dir", os.path.join(APP_MAIN, "res"), "-o", res_zip])
    return res_zip


APP_ID = "com.chromastrain.game"


def prepared_manifest():
    """The repo manifest is namespace-less (AGP 8 style); aapt2 needs the
    package attribute, so inject it into a build-dir copy."""
    src = os.path.join(APP_MAIN, "AndroidManifest.xml")
    with open(src, "r") as fh:
        xml = fh.read()
    if "package=" not in xml:
        xml = xml.replace("<manifest ", '<manifest package="%s" ' % APP_ID, 1)
    out = os.path.join(BUILD, "AndroidManifest.xml")
    with open(out, "w") as fh:
        fh.write(xml)
    return out


def link_resources(aapt2, res_zip):
    base_apk = os.path.join(BUILD, "base.apk")
    run([
        aapt2, "link",
        "-o", base_apk,
        "--manifest", prepared_manifest(),
        "-I", os.path.join(TOOLS, "android-all.jar"),
        "--min-sdk-version", MIN_SDK,
        "--target-sdk-version", TARGET_SDK,
        "--version-code", VERSION_CODE,
        "--version-name", VERSION_NAME,
        "--auto-add-overlay",
        res_zip,
    ])
    return base_apk


def compile_java():
    classes = os.path.join(BUILD, "classes")
    os.makedirs(classes, exist_ok=True)
    sources = []
    for dirpath, _dirnames, filenames in os.walk(os.path.join(APP_MAIN, "java")):
        for f in filenames:
            if f.endswith(".java"):
                sources.append(os.path.join(dirpath, f))
    if not sources:
        raise SystemExit("no java sources found")
    srclist = os.path.join(BUILD, "sources.txt")
    with open(srclist, "w") as fh:
        fh.write("\n".join(sources))
    run([
        "javac", "--release", "8", "-encoding", "UTF-8",
        "-Xlint:-options", "-nowarn",
        "-cp", os.path.join(TOOLS, "android-all.jar"),
        "-d", classes,
        "@" + srclist,
    ])
    return classes


def dex_classes(classes_dir):
    dex_out = os.path.join(BUILD, "dex")
    os.makedirs(dex_out, exist_ok=True)
    run([
        "java", "-cp", os.path.join(TOOLS, "dx.jar"),
        "com.android.dx.command.Main",
        "--dex", "--min-sdk-version=" + MIN_SDK,
        "--output=" + os.path.join(dex_out, "classes.dex"),
        classes_dir,
    ])
    return os.path.join(dex_out, "classes.dex")


class AlignedApkWriter(object):
    """Writes a zip where selected entries are STORED and 4-byte aligned
    (what zipalign would do), which Android 11+ requires for resources.arsc
    and AssetFileDescriptor requires for openable assets."""

    def __init__(self, path):
        self.zf = zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED)

    def add(self, name, data, stored):
        zi = zipfile.ZipInfo(name, date_time=(2024, 1, 1, 0, 0, 0))
        if stored:
            zi.compress_type = zipfile.ZIP_STORED
            offset = self.zf.fp.tell()
            header = 30 + len(name.encode("utf-8"))
            pad = (-(offset + header)) % 4
            if pad:
                # well-formed extra field: id 0xCAFE, zero payload of `pad-4`
                if pad < 4:
                    pad += 4
                zi.extra = struct.pack("<HH", 0xCAFE, pad - 4) + b"\x00" * (pad - 4)
        else:
            zi.compress_type = zipfile.ZIP_DEFLATED
        zi.external_attr = 0o100644 << 16
        self.zf.writestr(zi, data)

    def close(self):
        self.zf.close()


def package(base_apk, dex_file):
    unsigned = os.path.join(BUILD, "unsigned.apk")
    if os.path.exists(unsigned):
        os.remove(unsigned)
    w = AlignedApkWriter(unsigned)
    # 1) everything aapt2 linked (binary manifest, resources.arsc, res/)
    with zipfile.ZipFile(base_apk) as z:
        for info in z.infolist():
            data = z.read(info.filename)
            stored = info.filename == "resources.arsc"
            w.add(info.filename, data, stored)
    # 2) dex
    with open(dex_file, "rb") as fh:
        w.add("classes.dex", fh.read(), stored=False)
    # 3) assets
    assets_root = os.path.join(APP_MAIN, "assets")
    if os.path.isdir(assets_root):
        for dirpath, _dirnames, filenames in os.walk(assets_root):
            for f in sorted(filenames):
                full = os.path.join(dirpath, f)
                rel = os.path.relpath(full, assets_root).replace(os.sep, "/")
                with open(full, "rb") as fh:
                    data = fh.read()
                stored = f.lower().endswith(STORED_SUFFIXES)
                w.add("assets/" + rel, data, stored)
    w.close()
    return unsigned


def sign(unsigned, out_path):
    out_dir = os.path.join(BUILD, "signed")
    if os.path.isdir(out_dir):
        shutil.rmtree(out_dir)
    os.makedirs(out_dir)
    run([
        "java", "-jar", os.path.join(TOOLS, "uber-apk-signer.jar"),
        "--apks", unsigned,
        "--allowResign",
        "--out", out_dir,
    ])
    produced = None
    for f in os.listdir(out_dir):
        if f.endswith(".apk"):
            produced = os.path.join(out_dir, f)
            break
    if not produced:
        raise SystemExit("signer produced no apk")
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    shutil.copyfile(produced, out_path)
    return out_path


def main():
    out_path = os.path.join(DIST, "ChromaStrain-debug.apk")
    args = sys.argv[1:]
    if "--out" in args:
        out_path = os.path.abspath(args[args.index("--out") + 1])
    if os.path.isdir(BUILD):
        shutil.rmtree(BUILD)
    os.makedirs(BUILD)
    aapt2 = ensure_tools()
    res_zip = compile_resources(aapt2)
    base_apk = link_resources(aapt2, res_zip)
    classes = compile_java()
    dex_file = dex_classes(classes)
    unsigned = package(base_apk, dex_file)
    final = sign(unsigned, out_path)
    size = os.path.getsize(final)
    log("OK -> %s (%.2f MB)" % (final, size / 1048576.0))


if __name__ == "__main__":
    main()
