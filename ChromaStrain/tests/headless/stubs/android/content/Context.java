package android.content;

import java.util.HashMap;

/** Headless stub Context: in-memory prefs, null services. */
public class Context {
    public static final int MODE_PRIVATE = 0;
    public static final String VIBRATOR_SERVICE = "vibrator";

    private final HashMap<String, MemPrefs> prefs = new HashMap<String, MemPrefs>();

    public SharedPreferences getSharedPreferences(String name, int mode) {
        MemPrefs p = prefs.get(name);
        if (p == null) {
            p = new MemPrefs();
            prefs.put(name, p);
        }
        return p;
    }

    public Object getSystemService(String name) {
        return null;
    }

    public android.content.res.AssetManager getAssets() {
        return null;
    }

    public static class MemPrefs implements SharedPreferences {
        final HashMap<String, Object> map = new HashMap<String, Object>();

        public java.util.Map<String, ?> getAll() { return map; }
        public String getString(String k, String d) { return map.containsKey(k) ? (String) map.get(k) : d; }
        public java.util.Set<String> getStringSet(String k, java.util.Set<String> d) { return d; }
        public int getInt(String k, int d) { return map.containsKey(k) ? ((Integer) map.get(k)).intValue() : d; }
        public long getLong(String k, long d) { return map.containsKey(k) ? ((Long) map.get(k)).longValue() : d; }
        public float getFloat(String k, float d) { return map.containsKey(k) ? ((Float) map.get(k)).floatValue() : d; }
        public boolean getBoolean(String k, boolean d) { return map.containsKey(k) ? ((Boolean) map.get(k)).booleanValue() : d; }
        public boolean contains(String k) { return map.containsKey(k); }
        public Editor edit() { return new MemEditor(this); }
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }
    }

    public static class MemEditor implements SharedPreferences.Editor {
        private final MemPrefs p;

        MemEditor(MemPrefs p) { this.p = p; }

        public SharedPreferences.Editor putString(String k, String v) { p.map.put(k, v); return this; }
        public SharedPreferences.Editor putStringSet(String k, java.util.Set<String> v) { return this; }
        public SharedPreferences.Editor putInt(String k, int v) { p.map.put(k, Integer.valueOf(v)); return this; }
        public SharedPreferences.Editor putLong(String k, long v) { p.map.put(k, Long.valueOf(v)); return this; }
        public SharedPreferences.Editor putFloat(String k, float v) { p.map.put(k, Float.valueOf(v)); return this; }
        public SharedPreferences.Editor putBoolean(String k, boolean v) { p.map.put(k, Boolean.valueOf(v)); return this; }
        public SharedPreferences.Editor remove(String k) { p.map.remove(k); return this; }
        public SharedPreferences.Editor clear() { p.map.clear(); return this; }
        public boolean commit() { return true; }
        public void apply() { }
    }
}
