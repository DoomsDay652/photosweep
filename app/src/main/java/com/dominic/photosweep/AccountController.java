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

/** Optional email accounts. No photos, progress or purchase tokens are stored in Firebase. */
final class AccountController {
    private final Activity activity;
    private final Runnable changed;
    private FirebaseAuth auth;
    private boolean busy;
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
    private void note(String text) { if (!activity.isDestroyed()) Toast.makeText(activity, text, Toast.LENGTH_LONG).show(); }
    private void button(LinearLayout parent, String text, Runnable action) {
        Button button = new Button(activity); button.setText(text); parent.addView(button);
        button.setEnabled(!busy); button.setOnClickListener(v -> action.run());
    }
    void addControls(LinearLayout parent) {
        TextView status = new TextView(activity); status.setTextColor(0xffedf8f9);
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        status.setText(user == null ? "Account: Guest · sign-in is optional" : "Account: " + user.getEmail()
                + (user.isEmailVerified() ? " · verified" : " · email not verified")); parent.addView(status);
        if (auth == null) {
            TextView availability = new TextView(activity); availability.setTextColor(0xffaac1cd);
            availability.setText("Online accounts are not available in this build. You can use every photo-review feature as a guest."); parent.addView(availability);
        } else if (user == null) {
            button(parent, "Create account", () -> credentials(true, false));
            button(parent, "Sign in", () -> credentials(false, false));
            button(parent, "Forgot password", this::resetPassword);
        } else {
            if (!user.isEmailVerified()) {
                button(parent, "Send verification email", () -> { if (busy) return; busy=true;
                    user.sendEmailVerification().addOnCompleteListener(task -> finish(task.isSuccessful(), task.getException(), "Verification email sent.")); });
                button(parent, "Check verification", () -> { if (busy) return; busy=true;
                    user.reload().addOnCompleteListener(task -> finish(task.isSuccessful(), task.getException(), "Account refreshed.")); });
            }
            button(parent, "Sign out", () -> { if (!busy) { auth.signOut(); changed.run(); } });
            button(parent, "Delete account", () -> credentials(false, true));
        }
        if (PlayPolicy.validUrl(BuildConfig.DELETION_URL)) button(parent, "Account deletion website", () -> open(BuildConfig.DELETION_URL));
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
    private void credentials(boolean create, boolean deleting) {
        if (auth == null || busy) return;
        FirebaseUser current=auth.getCurrentUser(); if (deleting && current == null) return;
        LinearLayout form=form();
        TextView description=new TextView(activity); description.setText(deleting
                ? "Permanently delete your Photo Sweep login and its Firebase authentication record. Enter your password to confirm. Device photos, Android Trash, local guest progress and Play purchases are preserved. This cannot be undone."
                : "Optional email account. Firebase handles your email, password credentials and authentication identifiers. Photos and progress remain on-device. You can delete this account in Settings or on our deletion website."); form.addView(description);
        EditText email=field(form,"Email address",false); if (deleting) { email.setText(current.getEmail()); email.setEnabled(false); }
        EditText password=field(form,"Password",true);
        CheckBox consent=new CheckBox(activity); consent.setText("I have read the privacy policy and agree to create an account.");
        if (create) { button(form,"Read privacy policy",this::showPrivacy); form.addView(consent); }
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(deleting ? "Delete account permanently?" : create ? "Create account" : "Sign in")
                .setView(form).setNegativeButton("Cancel",null).setPositiveButton(deleting ? "Delete account" : create ? "Create" : "Sign in",null).create();
        dialog.setOnDismissListener(d -> password.setText("")); dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (busy) return;
            String address=email.getText().toString().trim(), secret=password.getText().toString();
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
                    user.sendEmailVerification().addOnCompleteListener(verification -> {
                        busy=false; note(verification.isSuccessful() ? "Account created. Check your email to verify it." : "Account created. Resend verification from Settings.");
                        if (!activity.isDestroyed()) changed.run();
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
        busy=false; String code=error instanceof FirebaseAuthException ? ((FirebaseAuthException)error).getErrorCode() : "";
        note(success ? message : code.equals("ERROR_TOO_MANY_REQUESTS") ? "Too many attempts. Please try again later."
                : "Could not complete this request. Check your connection and credentials, then try again.");
        if (!activity.isDestroyed()) changed.run();
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
