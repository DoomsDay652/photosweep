package com.dominic.photosweep;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/** Optional email accounts. No photos, progress or purchase tokens are stored in Firebase. */
final class AccountController {
    private final Activity activity;
    private final Runnable changed;
    private FirebaseAuth auth;
    private boolean busy;
    private int panel = 0xff23384d, ink = 0xffedf8f9, accent = 0xff40d2bc,
            muted = 0xffaac1cd, danger = 0xffff7580;
    private static final int SECONDARY = 0, PRIMARY = 1, DESTRUCTIVE = 2;
    private int dp(float value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private int blend(int first, int second, float amount) {
        return android.graphics.Color.rgb(
                Math.round(android.graphics.Color.red(first) * (1 - amount) + android.graphics.Color.red(second) * amount),
                Math.round(android.graphics.Color.green(first) * (1 - amount) + android.graphics.Color.green(second) * amount),
                Math.round(android.graphics.Color.blue(first) * (1 - amount) + android.graphics.Color.blue(second) * amount));
    }
    private android.graphics.drawable.GradientDrawable surface(int color, int outline) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{blend(color, android.graphics.Color.WHITE, .09f), color});
        shape.setCornerRadius(dp(18)); shape.setStroke(dp(1), outline); return shape;
    }
    private FirebaseAuth.AuthStateListener listener;

    AccountController(Activity activity, Runnable changed) {
        this.activity = activity; this.changed = changed;
        if (!PlayPolicy.accountReady(BuildConfig.FIREBASE_API_KEY, BuildConfig.FIREBASE_APP_ID,
                BuildConfig.FIREBASE_PROJECT_ID, BuildConfig.PRIVACY_URL, BuildConfig.DELETION_URL)) return;
        FirebaseApp app;
        try { app = FirebaseApp.getInstance("photosweep-accounts"); }
        catch (IllegalStateException absent) {
            app = FirebaseApp.initializeApp(activity.getApplicationContext(), new FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY).setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID).build(), "photosweep-accounts");
        }
        auth = FirebaseAuth.getInstance(app);
        listener = a -> { if (!activity.isDestroyed()) changed.run(); };
        auth.addAuthStateListener(listener);
    }
    void close() { if (auth != null && listener != null) auth.removeAuthStateListener(listener); }
    String photosHeading() {
        FirebaseUser user=auth == null ? null : auth.getCurrentUser();
        String name=user == null ? null : user.getDisplayName();
        return name == null || name.trim().isEmpty() ? "Your photos" : name.trim() + "’s photos";
    }
    private void note(String text) { if (!activity.isDestroyed()) Toast.makeText(activity, text, Toast.LENGTH_LONG).show(); }
    private void button(LinearLayout parent, String text, Runnable action) {
        button(parent, text, SECONDARY, action);
    }
    private void button(LinearLayout parent, String text, int role, Runnable action) {
        Button control = new Button(activity);
        control.setText(text); control.setAllCaps(false); control.setTextSize(16);
        control.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        control.setMinHeight(dp(56)); control.setMinimumHeight(dp(56));
        control.setPadding(dp(18), dp(14), dp(18), dp(14));
        int color = role == PRIMARY ? accent : role == DESTRUCTIVE ? blend(panel, danger, .18f) : panel;
        int outline = role == DESTRUCTIVE ? danger : role == PRIMARY ? accent : blend(panel, accent, .45f);
        control.setTextColor(role == PRIMARY ? 0xff17212b : ink);
        android.graphics.drawable.GradientDrawable mask = surface(android.graphics.Color.WHITE, android.graphics.Color.WHITE);
        control.setBackgroundTintList(null);
        control.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33ffffff), surface(color, outline), mask));
        control.setElevation(dp(2)); control.setEnabled(!busy); control.setAlpha(busy ? .55f : 1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10); parent.addView(control, params);
        control.setOnClickListener(v -> { if (!busy) action.run(); });
    }
    void addControls(LinearLayout parent) { addControls(parent, panel, ink, accent, muted, danger); }
    void addControls(LinearLayout parent, int panel, int ink, int accent, int muted, int danger) {
        this.panel = panel; this.ink = ink; this.accent = accent; this.muted = muted; this.danger = danger;
        TextView status = new TextView(activity); status.setTextColor(ink); status.setTextSize(16);
        status.setPadding(dp(18), dp(16), dp(18), dp(16));
        status.setBackground(surface(panel, blend(panel, accent, .35f)));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2); statusParams.bottomMargin = dp(16);
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        status.setText(user == null ? "Account: Guest · sign-in is optional" : "Account: "
                + (user.getDisplayName() == null || user.getDisplayName().isEmpty() ? "" : user.getDisplayName() + "\n") + user.getEmail()
                + (user.isEmailVerified() ? " · verified" : " · email not verified")); parent.addView(status, statusParams);
        if (auth == null) {
            TextView availability = new TextView(activity); availability.setTextColor(muted);
            availability.setText("Online accounts are not available in this build. You can use every photo-review feature as a guest."); parent.addView(availability);
        } else if (user == null) {
            button(parent, "Create account", PRIMARY, () -> credentials(true, false));
            button(parent, "Sign in", () -> credentials(false, false));
            button(parent, "Forgot password", this::resetPassword);
        } else {
            button(parent, "Edit username", user.isEmailVerified() ? PRIMARY : SECONDARY, this::editUsername);
            if (!user.isEmailVerified()) {
                button(parent, "Send verification email", PRIMARY, () -> { if (busy) return; busy=true;
                    user.sendEmailVerification().addOnCompleteListener(task -> finish(task.isSuccessful(), task.getException(), "Verification email sent.")); });
                button(parent, "Check verification", () -> { if (busy) return; busy=true;
                    user.reload().addOnCompleteListener(task -> finish(task.isSuccessful(), task.getException(), "Account refreshed.")); });
            }
            button(parent, "Sign out", () -> { if (!busy) { auth.signOut(); changed.run(); } });
            button(parent, "Delete account", DESTRUCTIVE, () -> credentials(false, true));
        }
        if (PlayPolicy.validUrl(BuildConfig.DELETION_URL)) button(parent, "Account deletion help ↗", () -> open(BuildConfig.DELETION_URL));
    }
    private LinearLayout form() {
        LinearLayout layout = new LinearLayout(activity); layout.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(20*activity.getResources().getDisplayMetrics().density); layout.setPadding(pad,pad,pad,pad); return layout;
    }
    private EditText field(LinearLayout form, String hint, boolean secret) {
        EditText input = new EditText(activity); input.setHint(hint);
        input.setInputType(secret ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setSaveEnabled(false); if (secret) input.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
        form.addView(input); return input;
    }
    private EditText usernameField(LinearLayout form, String value) {
        EditText input=new EditText(activity); input.setHint("Username (display name)");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});
        input.setSaveEnabled(false); if (value != null) input.setText(value); form.addView(input);
        TextView help=new TextView(activity); help.setText("3–24 letters, numbers or underscores. Sign in with your email; usernames are display names and may be shared by other users."); form.addView(help);
        return input;
    }
    private boolean validUsername(String name) {
        return name.matches("[A-Za-z0-9_]{3,24}");
    }
    private void editUsername() {
        if (auth == null || busy || auth.getCurrentUser() == null) return;
        FirebaseUser user=auth.getCurrentUser(); LinearLayout form=form();
        EditText username=usernameField(form,user.getDisplayName());
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Edit username").setView(form)
                .setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (busy) return; String name=username.getText().toString().trim();
            if (!validUsername(name)) { note("Use 3–24 letters, numbers or underscores for your username."); return; }
            busy=true; dialog.dismiss();
            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build())
                    .addOnCompleteListener(task -> finish(task.isSuccessful(),task.getException(),"Username saved."));
        })); dialog.show();
    }
    private void credentials(boolean create, boolean deleting) {
        if (auth == null || busy) return;
        FirebaseUser current=auth.getCurrentUser(); if (deleting && current == null) return;
        LinearLayout form=form();
        TextView description=new TextView(activity); description.setText(deleting
                ? "Permanently delete your Photo Sweep login and its Firebase authentication record. Enter your password to confirm. Device photos, Android Trash, local guest progress and Play purchases are preserved. This cannot be undone."
                : "Optional email account. Firebase stores your username, email, password credentials and authentication identifiers. Use your email to sign in. Photos and progress remain on-device. You can delete this account in Settings or on our deletion website."); form.addView(description);
        EditText username=create ? usernameField(form,null) : null;
        EditText email=field(form,"Email address",false); if (deleting) { email.setText(current.getEmail()); email.setEnabled(false); }
        EditText password=field(form,"Password",true);
        CheckBox consent=new CheckBox(activity); consent.setText("I have read the privacy policy and agree to create an account.");
        if (create) { button(form,"Read privacy policy",this::showPrivacy); form.addView(consent); }
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(deleting ? "Delete account permanently?" : create ? "Create account" : "Sign in")
                .setView(form).setNegativeButton("Cancel",null).setPositiveButton(deleting ? "Delete account" : create ? "Create" : "Sign in",null).create();
        dialog.setOnDismissListener(d -> password.setText("")); dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (busy) return;
            String address=email.getText().toString().trim(), secret=password.getText().toString();
            String name=create ? username.getText().toString().trim() : "";
            if (create && !validUsername(name)) { note("Use 3–24 letters, numbers or underscores for your username."); return; }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches() || secret.isEmpty()) { note("Enter a valid email and password."); return; }
            if (create && (!consent.isChecked() || secret.length()<8)) { note("Read the privacy policy and choose a password of at least 8 characters."); return; }
            busy=true; password.setText(""); dialog.dismiss();
            if (deleting) {
                String uid=current.getUid();
                current.reauthenticate(EmailAuthProvider.getCredential(address,secret)).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) { finish(false,task.getException(),""); return; }
                    FirebaseUser refreshed=auth.getCurrentUser();
                    if (refreshed == null || !uid.equals(refreshed.getUid())) { finish(false,null,""); return; }
                    refreshed.delete().addOnCompleteListener(result -> finish(result.isSuccessful(), result.getException(),"Account deleted. You are now a guest."));
                });
            } else if (create) {
                auth.createUserWithEmailAndPassword(address,secret).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) { finish(false,task.getException(),""); return; }
                    FirebaseUser user=auth.getCurrentUser(); if (user == null) { finish(false,null,""); return; }
                    user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build()).addOnCompleteListener(profile -> {
                        if (!profile.isSuccessful()) { finish(false,profile.getException(),""); return; }
                        user.sendEmailVerification().addOnCompleteListener(verification -> {
                            finish(verification.isSuccessful(), verification.getException(),
                                    "Account created. Check your email to verify it.");
                        });
                    });
                });
            } else auth.signInWithEmailAndPassword(address,secret).addOnCompleteListener(task -> finish(task.isSuccessful(),task.getException(),"Signed in."));
        })); dialog.show();
    }
    private void resetPassword() {
        if (auth == null || busy) return;
        LinearLayout form=form(); EditText email=field(form,"Email address",false);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Reset password").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Send reset email",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String address=email.getText().toString().trim();
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()) { note("Enter a valid email address."); return; }
            if (busy) return; busy=true; dialog.dismiss();
            auth.sendPasswordResetEmail(address).addOnCompleteListener(task -> finish(task.isSuccessful(),task.getException(),"If an account exists, a password reset email has been sent."));
        })); dialog.show();
    }
    private void finish(boolean success, Exception error, String message) {
        busy=false;
        if (success) note(message);
        else if (!activity.isDestroyed()) new AlertDialog.Builder(activity)
                .setTitle("Account request failed")
                .setMessage(accountError(error))
                .setPositiveButton("OK", null).show();
        if (!activity.isDestroyed()) changed.run();
    }
    private String accountError(Exception error) {
        if (error instanceof com.google.firebase.FirebaseNetworkException)
            return "Firebase could not be reached. Check your internet connection and try again.";
        if (error instanceof com.google.firebase.FirebaseTooManyRequestsException)
            return "Firebase temporarily blocked further attempts. Wait before trying again.";
        String code=error instanceof FirebaseAuthException ? ((FirebaseAuthException)error).getErrorCode() : "";
        switch (code) {
            case "ERROR_WRONG_PASSWORD":
            case "ERROR_INVALID_CREDENTIAL":
            case "ERROR_INVALID_LOGIN_CREDENTIALS":
                return "Firebase could not verify your email and password. For deletion, enter your Photo Sweep account password. If you forgot it, sign out and use Forgot password, then sign in again.";
            case "ERROR_REQUIRES_RECENT_LOGIN":
            case "ERROR_USER_TOKEN_EXPIRED":
            case "ERROR_INVALID_USER_TOKEN":
                return "Your sign-in needs to be refreshed. Sign out, sign in again, and retry the request.";
            case "ERROR_USER_NOT_FOUND":
                return "This account no longer exists in Firebase. Sign out to return to guest mode.";
            case "ERROR_USER_DISABLED":
                return "This account has been disabled. Contact Dreamy Game Studios support.";
            case "ERROR_EMAIL_ALREADY_IN_USE":
                return "An account already uses this email. Choose Sign in or Forgot password.";
            case "ERROR_TOO_MANY_REQUESTS":
                return "Firebase temporarily blocked further attempts. Wait before trying again.";
            case "ERROR_OPERATION_NOT_ALLOWED":
                return "Firebase has not enabled this account operation. Contact Dreamy Game Studios support.";
            default:
                return "Firebase could not complete the request. Your account may still exist. Try signing out and back in. If it continues, send support this code: "
                        + (code.isEmpty() ? (error == null ? "NO_CURRENT_USER" : error.getClass().getSimpleName()) : code) + ".";
        }
    }
    private void open(String url) {
        try { activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))); }
        catch (android.content.ActivityNotFoundException e) { note("No browser is available to open this page."); }
    }
    void showPrivacy() {
        if (PlayPolicy.validUrl(BuildConfig.PRIVACY_URL)) { open(BuildConfig.PRIVACY_URL); return; }
        new AlertDialog.Builder(activity).setTitle("Photo Sweep privacy")
            .setMessage("This build processes permitted photos on your device. It saves local review progress and Trash recovery records. It does not upload photos, create online accounts, show ads or make purchases. Android controls photo access, Trash and Restore permissions. You can revoke access in Android app settings. Reset local progress clears review preferences; it preserves photos and Trash records. Uninstalling removes local app data. A public privacy policy and contact details must be configured before store release.")
            .setPositiveButton("Close",null).show();
    }
}
