package com.novia.home.web;
import android.webkit.JavascriptInterface;
/** Bridge for the bundled Phase 5 recovery UI; no relay commands are exposed here. */
public final class Phase5Bridge {
 public interface Actions { void scan(); void openLocal(); void openOnline(); }
 private final Actions actions;
 public Phase5Bridge(Actions actions){this.actions=actions;}
 @JavascriptInterface public void scan(){if(actions!=null)actions.scan();}
 @JavascriptInterface public void openLocal(){if(actions!=null)actions.openLocal();}
 @JavascriptInterface public void openOnline(){if(actions!=null)actions.openOnline();}
}
