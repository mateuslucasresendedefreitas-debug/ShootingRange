package android.graphics;

/** Headless stub: shadows the android-all Paint (native) for JVM simulation. */
public class Paint {
    public static final int ANTI_ALIAS_FLAG = 1;

    public enum Style { FILL, STROKE, FILL_AND_STROKE }
    public enum Align { LEFT, CENTER, RIGHT }
    public enum Cap { BUTT, ROUND, SQUARE }
    public enum Join { MITER, ROUND, BEVEL }

    public Paint() { }
    public Paint(int flags) { }
    public void setColor(int c) { }
    public void setStyle(Style s) { }
    public void setStrokeWidth(float w) { }
    public void setStrokeCap(Cap c) { }
    public void setStrokeJoin(Join j) { }
    public void setTextSize(float s) { }
    public void setTextAlign(Align a) { }
    public Typeface setTypeface(Typeface t) { return t; }
    public Shader setShader(Shader s) { return s; }
    public void setShadowLayer(float r, float dx, float dy, int color) { }
    public void clearShadowLayer() { }
    public float measureText(String s) { return s == null ? 0 : s.length() * 8f; }
    public float getTextSize() { return 16f; }
}
