package com.dominic.photosweep;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.CancellationSignal;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.CustomCredential;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.ClearCredentialException;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.AuthCredential;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.Purchase;
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

/** Optional Google and email accounts. No photos, progress or purchase tokens are stored in Firebase. */
final class AccountController {
    private final Activity activity;
    private final Runnable changed;
    private FirebaseAuth auth;
    private boolean busy, closed;
    private CancellationSignal credentialCancellation;
    private BillingClient restoreClient;
    private boolean restoring;
    private int restoreSession;
    private final android.os.Handler restoreHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable restoreTimeout = () -> {
        if (restoring && !closed) restored("Google Play took too long to respond. Please try again.");
    };
    private boolean linkedGoogle(FirebaseUser user) {
        return user != null && user.getProviderData().stream()
                .anyMatch(provider -> GoogleAuthProvider.PROVIDER_ID.equals(provider.getProviderId()));
    }
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
    void close() {
        closed = true;
        restoreHandler.removeCallbacks(restoreTimeout);
        if (credentialCancellation != null) credentialCancellation.cancel();
        if (restoreClient != null) restoreClient.endConnection();
        if (auth != null && listener != null) auth.removeAuthStateListener(listener);
    }
    String photosHeading() {
        FirebaseUser user=auth == null ? null : auth.getCurrentUser();
        String name=user == null ? null : user.getDisplayName();
        return name == null || name.trim().isEmpty() ? "Your photos" : name.trim() + "’s photos";
    }
    private void note(String text) { if (!activity.isDestroyed()) Toast.makeText(activity, text, Toast.LENGTH_LONG).show(); }
    private void button(LinearLayout parent, String text, Runnable action) {
        button(parent, text, SECONDARY, action);
    }
    private Button button(LinearLayout parent, String text, int role, Runnable action) {
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
        return control;
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
            com.google.android.gms.common.SignInButton google = new com.google.android.gms.common.SignInButton(activity);
            google.setSize(com.google.android.gms.common.SignInButton.SIZE_WIDE);
            google.setColorScheme(com.google.android.gms.common.SignInButton.COLOR_LIGHT);
            google.setEnabled(!busy);
            LinearLayout.LayoutParams googleParams = new LinearLayout.LayoutParams(-1, dp(56));
            googleParams.bottomMargin = dp(10); parent.addView(google, googleParams);
            google.setOnClickListener(v -> { if (!busy) googleConsent(false); });
            button(parent, "Create account with email", () -> credentials(true, false));
            button(parent, "Sign in with email", () -> credentials(false, false));
            button(parent, "Forgot password", this::resetPassword);
        } else {
            button(parent, "Edit username", user.isEmailVerified() ? PRIMARY : SECONDARY, this::editUsername);
            if (!user.isEmailVerified()) {
                button(parent, "Send verification email", PRIMARY, () -> { if (busy) return; busy=true;
                    user.sendEmailVerification().addOnCompleteListener(task -> finish(task.isSuccessful(), task.getException(), "Verification email sent.")); });
                button(parent, "Check verification", () -> { if (busy) return; busy=true;
                    user.reload().addOnCompleteListener(task -> finish(task.isSuccessful(), task.getException(), "Account refreshed.")); });
            }
            if (!linkedGoogle(user)) button(parent, "Link Google account", () -> googleConsent(true));
            button(parent, "Sign out", this::signOut);
            button(parent, "Delete account", DESTRUCTIVE, () -> {
                if (linkedGoogle(user)) confirmGoogleDeletion(); else credentials(false, true);
            });
        }
        button(parent, restoring ? "Checking purchases…" : "Restore purchases", this::restorePurchases);
        if (PlayPolicy.validUrl(BuildConfig.DELETION_URL)) button(parent, "Account deletion help ↗", () -> open(BuildConfig.DELETION_URL));
    }
    private void googleConsent(boolean linking) {
        if (busy || auth == null) return;
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty()) {
            note("Google sign-in is awaiting studio configuration. Email sign-in and guest mode are available."); return;
        }
        LinearLayout content = form();
        TextView explanation = new TextView(activity);
        explanation.setText(linking ? "Add Google sign-in to this account. Your username stays the same."
                : "Use your Google account. Your photos and progress stay on this device.");
        explanation.setTextColor(muted); explanation.setPadding(0, 0, 0, dp(12)); content.addView(explanation);
        button(content, "Privacy policy", this::showPrivacy);
        CheckBox consent = new CheckBox(activity); consent.setText("I agree to the privacy policy.");
        consent.setTextColor(ink); consent.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));
        content.addView(consent);
        AlertDialog dialog = accountDialog(linking ? "Link Google account" : "Continue with Google",
                content, "Continue", false);
        dialog.setOnShowListener(d -> dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            if (!consent.isChecked()) { note("Please agree to the privacy policy to continue."); return; }
            dialog.dismiss(); googleCredential(linking ? 1 : 0);
        })); showAccountDialog(dialog);
    }
    private void confirmGoogleDeletion() {
        LinearLayout content = form(); TextView explanation = new TextView(activity);
        explanation.setText("Permanently delete your Photo Sweep account? Your Google account, photos and Google Play purchases stay intact.");
        explanation.setTextColor(muted); explanation.setPadding(0, 0, 0, dp(16)); content.addView(explanation);
        AlertDialog dialog = accountDialog("Delete Photo Sweep account?", content, "Verify with Google and delete", true);
        dialog.setOnShowListener(d -> dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            if (busy) return; dialog.dismiss(); googleCredential(2);
        })); showAccountDialog(dialog);
    }
    // 0: sign in, 1: link to the authenticated account, 2: reauthenticate then delete.
    private void googleCredential(int operation) {
        if (busy || auth == null || closed) return;
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty()) { note("Google sign-in needs studio configuration."); return; }
        FirebaseUser original = auth.getCurrentUser();
        if (operation != 0 && original == null) return;
        String originalId = original == null ? null : original.getUid();
        busy = true; credentialCancellation = new CancellationSignal();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(new GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()).build();
        CredentialManager.create(activity).getCredentialAsync(activity, request, credentialCancellation,
                activity.getMainExecutor(), new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
            @Override public void onResult(GetCredentialResponse result) {
                if (closed || activity.isDestroyed()) return;
                try {
                    if (!(result.getCredential() instanceof CustomCredential)) throw new IllegalArgumentException("Unexpected credential");
                    CustomCredential custom = (CustomCredential) result.getCredential();
                    if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(custom.getType()))
                        throw new IllegalArgumentException("Unexpected credential type");
                    AuthCredential credential = GoogleAuthProvider.getCredential(
                            GoogleIdTokenCredential.createFrom(custom.getData()).getIdToken(), null);
                    if (operation == 0) {
                        auth.signInWithCredential(credential).addOnCompleteListener(task -> finish(task.isSuccessful(), task.getException(), "Signed in with Google."));
                        return;
                    }
                    FirebaseUser current = auth.getCurrentUser();
                    if (current == null || !current.getUid().equals(originalId)) { finish(false, null, ""); return; }
                    if (operation == 1) {
                        String savedName = current.getDisplayName();
                        current.linkWithCredential(credential).addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) { finish(false, task.getException(), ""); return; }
                            if (savedName == null || savedName.isEmpty()) { finish(true, null, "Google account linked."); return; }
                            current.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(savedName).build())
                                    .addOnCompleteListener(profile -> finish(profile.isSuccessful(), profile.getException(), "Google account linked."));
                        });
                    } else current.reauthenticate(credential).addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) { finish(false, task.getException(), ""); return; }
                        FirebaseUser refreshed = auth.getCurrentUser();
                        if (refreshed == null || !refreshed.getUid().equals(originalId)) { finish(false, null, ""); return; }
                        refreshed.delete().addOnCompleteListener(deleted -> {
                            if (deleted.isSuccessful()) clearGoogleSession();
                            finish(deleted.isSuccessful(), deleted.getException(), "Photo Sweep account deleted. You are now a guest.");
                        });
                    });
                } catch (Exception invalid) { finish(false, invalid, ""); }
            }
            @Override public void onError(GetCredentialException error) {
                if (closed || activity.isDestroyed()) return;
                if (error instanceof GetCredentialCancellationException) { busy = false; changed.run(); return; }
                busy = false;
                note("Google sign-in could not finish. Check your connection and Google account, then try again."); changed.run();
            }
        });
    }
    private void signOut() {
        if (busy || auth == null) return;
        auth.signOut(); clearGoogleSession(); changed.run();
    }
    private void clearGoogleSession() {
        CredentialManager.create(activity).clearCredentialStateAsync(new ClearCredentialStateRequest(), null,
                activity.getMainExecutor(), new CredentialManagerCallback<Void, ClearCredentialException>() {
            @Override public void onResult(Void result) { }
            @Override public void onError(ClearCredentialException error) {
                if (!closed) note("Signed out. If Google offers the previous account, choose a different account.");
            }
        });
    }
    private void restorePurchases() {
        if (restoring || closed) return;
        restoring = true; final int session = ++restoreSession;
        restoreHandler.postDelayed(restoreTimeout, 30000); changed.run();
        restoreClient = BillingClient.newBuilder(activity.getApplicationContext())
                .setListener((result, purchases) -> {})
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection().build();
        restoreClient.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(BillingResult result) {
                if (closed || session != restoreSession || !restoring) return;
                if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    restored("Google Play could not be reached. Check that Play Store is installed and signed in, then try again."); return;
                }
                restoreClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP).build(), (query, purchases) -> {
                    if (closed || session != restoreSession || !restoring) return;
                    if (query.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        restored("Purchases could not be checked. Try again when Google Play is available."); return;
                    }
                    boolean removeAds = false, pending = false;
                    for (Purchase purchase : purchases) {
                        if (!purchase.getProducts().contains(AppServices.Product.REMOVE_ADS.playId)) continue;
                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && purchase.isAcknowledged()) removeAds = true;
                        else pending = true;
                    }
                    activity.getSharedPreferences("billing_entitlements", Activity.MODE_PRIVATE).edit()
                            .putBoolean("remove_ads", removeAds).apply();
                    restored(removeAds ? "Ad removal restored from Google Play."
                            : pending ? "Ad removal is pending payment or purchase verification. Try again after it completes."
                            : "No ad-removal purchase was found for this Google Play account. Payments are not available yet.");
                });
            }
            @Override public void onBillingServiceDisconnected() {
                if (!closed && session == restoreSession && restoring) restored("Google Play disconnected. Please try restoring again.");
            }
        });
    }
    private void restored(String message) {
        activity.runOnUiThread(() -> {
            if (closed) return;
            restoring = false; restoreSession++; restoreHandler.removeCallbacks(restoreTimeout);
            if (restoreClient != null) { restoreClient.endConnection(); restoreClient = null; }
            note(message); changed.run();
        });
    }
    private AlertDialog accountDialog(String title, LinearLayout form, String action, boolean deleting) {
        LinearLayout content = new LinearLayout(activity); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(12));
        content.setBackground(surface(panel, blend(panel, accent, .45f)));
        TextView heading = new TextView(activity); heading.setText(title); heading.setTextColor(ink);
        heading.setTextSize(24); heading.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        heading.setPadding(0, 0, 0, dp(16)); content.addView(heading);
        android.widget.ScrollView scroll = new android.widget.ScrollView(activity) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                int max = (int)(activity.getResources().getDisplayMetrics().heightPixels * .55f);
                super.onMeasure(widthSpec, android.view.View.MeasureSpec.makeMeasureSpec(max, android.view.View.MeasureSpec.AT_MOST));
            }
        };
        scroll.setClipToPadding(false); scroll.addView(form); content.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
        AlertDialog dialog = new AlertDialog.Builder(activity).setView(content).create();
        Button submit = button(content, action, deleting ? DESTRUCTIVE : PRIMARY, () -> {});
        submit.setId(android.R.id.button1);
        button(content, "Cancel", SECONDARY, dialog::dismiss);
        return dialog;
    }
    private void showAccountDialog(AlertDialog dialog) {
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(activity.getResources().getDisplayMetrics().widthPixels - dp(32), -2);
        }
    }
    private void styleInput(EditText input, LinearLayout form) {
        input.setTextColor(ink); input.setHintTextColor(muted); input.setTextSize(16);
        input.setPadding(dp(14), dp(14), dp(14), dp(14)); input.setMinHeight(dp(56));
        input.setBackgroundTintList(null);
        input.setBackground(surface(blend(panel, android.graphics.Color.BLACK, .18f), blend(panel, accent, .5f)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(12);
        form.addView(input, params);
    }
    private LinearLayout form() {
        LinearLayout layout = new LinearLayout(activity); layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 0, 0, dp(8)); return layout;
    }
    private EditText field(LinearLayout form, String hint, boolean secret) {
        EditText input = new EditText(activity); input.setHint(hint);
        input.setInputType(secret ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setSaveEnabled(false); if (secret) input.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
        styleInput(input, form); return input;
    }
    private EditText usernameField(LinearLayout form, String value) {
        EditText input=new EditText(activity); input.setHint("Username");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});
        input.setSaveEnabled(false); if (value != null) input.setText(value); styleInput(input, form);
        TextView help=new TextView(activity); help.setText("3–24 letters, numbers or underscores."); help.setTextColor(muted); help.setTextSize(13); help.setPadding(0, 0, 0, dp(12)); form.addView(help);
        return input;
    }
    private boolean validUsername(String name) {
        return name.matches("[A-Za-z0-9_]{3,24}");
    }
    private void editUsername() {
        if (auth == null || busy || auth.getCurrentUser() == null) return;
        FirebaseUser user=auth.getCurrentUser(); LinearLayout form=form();
        EditText username=usernameField(form,user.getDisplayName());
        AlertDialog dialog=accountDialog("Edit username", form, "Save", false);
        dialog.setOnShowListener(d -> dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            if (busy) return; String name=username.getText().toString().trim();
            if (!validUsername(name)) { note("Use 3–24 letters, numbers or underscores for your username."); return; }
            busy=true; dialog.dismiss();
            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build())
                    .addOnCompleteListener(task -> finish(task.isSuccessful(),task.getException(),"Username saved."));
        })); showAccountDialog(dialog);
    }
    private void credentials(boolean create, boolean deleting) {
        if (auth == null || busy) return;
        FirebaseUser current=auth.getCurrentUser(); if (deleting && current == null) return;
        LinearLayout form=form();
        TextView description=new TextView(activity); description.setText(deleting
                ? "Enter your password to permanently delete your account. Photos, local progress and purchases stay intact."
                : create ? "Create an optional account. Your photos stay on this device." : "Sign in with your email and password."); description.setTextColor(muted); description.setTextSize(14); description.setPadding(0,0,0,dp(16)); form.addView(description);
        EditText username=create ? usernameField(form,null) : null;
        EditText email=field(form,"Email address",false); if (deleting) { email.setText(current.getEmail()); email.setEnabled(false); }
        EditText password=field(form,"Password",true);
        CheckBox consent=new CheckBox(activity); consent.setText("I agree to the privacy policy."); consent.setTextColor(ink); consent.setTextSize(14); consent.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));
        if (create) { button(form,"Read privacy policy",this::showPrivacy); form.addView(consent); }
        AlertDialog dialog=accountDialog(deleting ? "Delete account?" : create ? "Create account" : "Sign in",
                form, deleting ? "Delete account" : create ? "Create account" : "Sign in", deleting);
        dialog.setOnDismissListener(d -> password.setText("")); dialog.setOnShowListener(d -> dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
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
        })); showAccountDialog(dialog);
    }
    private void resetPassword() {
        if (auth == null || busy) return;
        LinearLayout form=form(); EditText email=field(form,"Email address",false);
        AlertDialog dialog=accountDialog("Reset password", form, "Send reset email", false);
        dialog.setOnShowListener(d -> dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String address=email.getText().toString().trim();
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()) { note("Enter a valid email address."); return; }
            if (busy) return; busy=true; dialog.dismiss();
            auth.sendPasswordResetEmail(address).addOnCompleteListener(task -> finish(task.isSuccessful(),task.getException(),"If an account exists, a password reset email has been sent."));
        })); showAccountDialog(dialog);
    }
    private void finish(boolean success, Exception error, String message) {
        busy=false;
        if (closed) return;
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
            case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
                return "This email already has a Photo Sweep account. Sign in with your existing email and password, then choose Link Google account.";
            case "ERROR_CREDENTIAL_ALREADY_IN_USE":
                return "That Google account is already linked to another Photo Sweep account. Sign out and use Sign in with Google.";
            case "ERROR_USER_MISMATCH":
                return "Choose the Google account linked to this Photo Sweep account to verify deletion.";
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
