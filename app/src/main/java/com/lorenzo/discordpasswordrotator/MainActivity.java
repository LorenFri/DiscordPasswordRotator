package com.lorenzo.discordpasswordrotator;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;
import java.util.Arrays;

public class MainActivity extends Activity {
    private static final int PASSWORD_LENGTH = 32;
    private static final long CLIPBOARD_CLEAR_DELAY_MS = 5 * 60 * 1000L;

    private static final char[] LOWER = "abcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final char[] UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] DIGITS = "23456789".toCharArray();
    private static final char[] SYMBOLS = "!@#$%^&*()-_=+".toCharArray();
    private static final char[] ALL = (
            "abcdefghijkmnopqrstuvwxyz" +
            "ABCDEFGHJKLMNPQRSTUVWXYZ" +
            "23456789" +
            "!@#$%^&*()-_=+"
    ).toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private char[] currentPassword;
    private boolean revealed = false;

    private TextView passwordView;
    private TextView statusView;
    private Button revealButton;
    private Button openDiscordButton;
    private Button copyButton;
    private Button clearButton;

    private final Runnable clearClipboardRunnable = this::clearClipboardOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(buildUi());
        updateUiNoPassword();
    }

    private View buildUi() {
        int bg = Color.rgb(16, 19, 26);
        int panel = Color.rgb(23, 27, 36);
        int primary = Color.rgb(243, 244, 246);
        int secondary = Color.rgb(183, 190, 204);
        int accent = Color.rgb(88, 101, 242);
        int danger = Color.rgb(237, 66, 69);
        int outline = Color.rgb(52, 58, 70);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(28));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(this);
        title.setText("DISCORD PASSWORD ROTATOR");
        title.setTextColor(primary);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(0, 0, 0, 8));

        TextView subtitle = new TextView(this);
        subtitle.setText("Generatore locale • nessuna rete • nessun salvataggio su disco");
        subtitle.setTextColor(secondary);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, matchWrap(0, 0, 0, 24));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(makeRounded(panel, 18, 0));
        root.addView(card, matchWrap(0, 0, 0, 18));

        TextView label = new TextView(this);
        label.setText("Nuova password");
        label.setTextColor(secondary);
        label.setTextSize(13);
        card.addView(label, matchWrap(0, 0, 0, 8));

        passwordView = new TextView(this);
        passwordView.setTextColor(primary);
        passwordView.setTextSize(18);
        passwordView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        passwordView.setGravity(Gravity.CENTER_VERTICAL);
        passwordView.setPadding(dp(14), dp(14), dp(14), dp(14));
        passwordView.setBackground(makeRounded(Color.rgb(12, 15, 21), 12, outline));
        passwordView.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passwordView.setTextIsSelectable(false);
        card.addView(passwordView, matchWrap(0, 0, 0, 12));

        LinearLayout smallButtons = new LinearLayout(this);
        smallButtons.setOrientation(LinearLayout.HORIZONTAL);
        smallButtons.setGravity(Gravity.CENTER);
        card.addView(smallButtons, matchWrap(0, 0, 0, 0));

        revealButton = createButton("MOSTRA", outline, primary);
        revealButton.setOnClickListener(v -> toggleReveal());
        smallButtons.addView(revealButton, weightedButton(1f, 0, 0, 6, 0));

        copyButton = createButton("COPIA", outline, primary);
        copyButton.setOnClickListener(v -> copyPasswordToClipboard());
        smallButtons.addView(copyButton, weightedButton(1f, 6, 0, 0, 0));

        Button generateButton = createButton("GENERA NUOVA PASSWORD", accent, Color.WHITE);
        generateButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        generateButton.setOnClickListener(v -> generateNewPassword());
        root.addView(generateButton, matchWrap(0, 0, 0, 12));

        openDiscordButton = createButton("COPIA E APRI DISCORD", accent, Color.WHITE);
        openDiscordButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        openDiscordButton.setOnClickListener(v -> {
            if (copyPasswordToClipboard()) {
                openDiscordPasswordSettings();
            }
        });
        root.addView(openDiscordButton, matchWrap(0, 0, 0, 10));

        Button compatButton = createButton("APRI ACCOUNT DISCORD (COMPATIBILITÀ)", outline, primary);
        compatButton.setOnClickListener(v -> openDiscordAccountSettings());
        root.addView(compatButton, matchWrap(0, 0, 0, 14));

        clearButton = createButton("CANCELLA PASSWORD E APPUNTI", Color.TRANSPARENT, danger);
        clearButton.setOnClickListener(v -> clearEverything());
        root.addView(clearButton, matchWrap(0, 0, 0, 18));

        statusView = new TextView(this);
        statusView.setTextColor(secondary);
        statusView.setTextSize(13);
        statusView.setGravity(Gravity.CENTER);
        root.addView(statusView, matchWrap(0, 0, 0, 24));

        TextView instructionsTitle = new TextView(this);
        instructionsTitle.setText("COME USARLA");
        instructionsTitle.setTextColor(primary);
        instructionsTitle.setTextSize(15);
        instructionsTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(instructionsTitle, matchWrap(0, 0, 0, 10));

        TextView instructions = new TextView(this);
        instructions.setText(
                "1. Premi GENERA NUOVA PASSWORD.\n\n" +
                "2. Premi COPIA E APRI DISCORD. Se la scorciatoia diretta non funziona, usa il pulsante di compatibilità e poi Account → Password.\n\n" +
                "3. Per Password attuale usa Samsung Pass. Nel campo Nuova password incolla quella copiata.\n\n" +
                "4. Conferma il cambio in Discord e l'eventuale MFA. Se Samsung Pass propone Aggiorna password, accetta.\n\n" +
                "5. Torna qui e premi CANCELLA PASSWORD E APPUNTI."
        );
        instructions.setTextColor(secondary);
        instructions.setTextSize(14);
        instructions.setLineSpacing(0f, 1.15f);
        root.addView(instructions, matchWrap(0, 0, 0, 22));

        TextView privacy = new TextView(this);
        privacy.setText(
                "Privacy: l'app non richiede INTERNET, non salva la password in file/preferenze/database e blocca screenshot e anteprime recenti. La password esiste soltanto in memoria e, quando scegli di copiarla, nella clipboard Android."
        );
        privacy.setTextColor(Color.rgb(145, 154, 171));
        privacy.setTextSize(12);
        privacy.setGravity(Gravity.CENTER);
        root.addView(privacy, matchWrap(0, 0, 0, 0));

        return scroll;
    }

    private void generateNewPassword() {
        wipePassword();

        char[] pwd = new char[PASSWORD_LENGTH];
        int pos = 0;
        pwd[pos++] = randomChar(LOWER);
        pwd[pos++] = randomChar(UPPER);
        pwd[pos++] = randomChar(DIGITS);
        pwd[pos++] = randomChar(SYMBOLS);
        while (pos < pwd.length) {
            pwd[pos++] = randomChar(ALL);
        }

        for (int i = pwd.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char t = pwd[i];
            pwd[i] = pwd[j];
            pwd[j] = t;
        }

        currentPassword = pwd;
        revealed = false;
        passwordView.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passwordView.setText(new String(currentPassword));
        revealButton.setText("MOSTRA");
        statusView.setText("Password generata localmente: 32 caratteri. Non è stata salvata su disco.");
        setActionButtonsEnabled(true);
    }

    private char randomChar(char[] source) {
        return source[secureRandom.nextInt(source.length)];
    }

    private void toggleReveal() {
        if (currentPassword == null) return;
        revealed = !revealed;
        if (revealed) {
            passwordView.setTransformationMethod(null);
            revealButton.setText("NASCONDI");
        } else {
            passwordView.setTransformationMethod(PasswordTransformationMethod.getInstance());
            revealButton.setText("MOSTRA");
        }
    }

    private boolean copyPasswordToClipboard() {
        if (currentPassword == null) {
            Toast.makeText(this, "Genera prima una password.", Toast.LENGTH_SHORT).show();
            return false;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Nuova password Discord", new String(currentPassword));

        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
        clip.getDescription().setExtras(extras);

        clipboard.setPrimaryClip(clip);

        handler.removeCallbacks(clearClipboardRunnable);
        handler.postDelayed(clearClipboardRunnable, CLIPBOARD_CLEAR_DELAY_MS);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toast.makeText(this, "Password copiata negli appunti.", Toast.LENGTH_SHORT).show();
        }
        statusView.setText("Password copiata come contenuto sensibile. La clipboard sarà ripulita tra circa 5 minuti.");
        return true;
    }

    private void openDiscordPasswordSettings() {
        if (!tryOpenDiscordUri("discord://-/settings/account_change_password")) {
            openDiscordAccountSettings();
        }
    }

    private void openDiscordAccountSettings() {
        if (!tryOpenDiscordUri("discord://-/settings/account")) {
            openDiscordHomeOrStore();
        }
    }

    private boolean tryOpenDiscordUri(String uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.discord");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    private void openDiscordHomeOrStore() {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage("com.discord");
            if (launch != null) {
                startActivity(launch);
                Toast.makeText(this, "Apri Impostazioni → Account → Password.", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception ignored) {
        }

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.discord")));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.discord")));
        }
    }

    private void clearEverything() {
        handler.removeCallbacks(clearClipboardRunnable);
        clearClipboardOnly();
        wipePassword();
        updateUiNoPassword();
        Toast.makeText(this, "Password e appunti cancellati.", Toast.LENGTH_SHORT).show();
    }

    private void clearClipboardOnly() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip();
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
            }
            if (statusView != null && currentPassword != null) {
                statusView.setText("Clipboard cancellata. La password resta disponibile nell'app finché non la cancelli.");
            }
        } catch (Exception ignored) {
        }
    }

    private void wipePassword() {
        if (currentPassword != null) {
            Arrays.fill(currentPassword, '\0');
            currentPassword = null;
        }
        if (passwordView != null) {
            passwordView.setText("");
        }
        revealed = false;
    }

    private void updateUiNoPassword() {
        if (passwordView != null) {
            passwordView.setText("Nessuna password generata");
            passwordView.setTransformationMethod(null);
        }
        if (statusView != null) {
            statusView.setText("Pronta. Genera una password quando vuoi iniziare.");
        }
        if (revealButton != null) revealButton.setText("MOSTRA");
        setActionButtonsEnabled(false);
    }

    private void setActionButtonsEnabled(boolean enabled) {
        if (revealButton != null) revealButton.setEnabled(enabled);
        if (copyButton != null) copyButton.setEnabled(enabled);
        if (openDiscordButton != null) openDiscordButton.setEnabled(enabled);
        if (clearButton != null) clearButton.setEnabled(enabled);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(clearClipboardRunnable);
        if (isFinishing()) {
            wipePassword();
        }
        super.onDestroy();
    }

    private Button createButton(String text, int backgroundColor, int textColor) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(textColor);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        button.setBackground(makeRounded(backgroundColor, 12, Color.TRANSPARENT));
        button.setMinHeight(dp(52));
        return button;
    }

    private android.graphics.drawable.Drawable makeRounded(int fillColor, int radiusDp, int strokeColor) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(radiusDp));
        if (strokeColor != Color.TRANSPARENT) {
            gd.setStroke(dp(1), strokeColor);
        }
        return gd;
    }

    private LinearLayout.LayoutParams matchWrap(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }

    private LinearLayout.LayoutParams weightedButton(float weight, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
