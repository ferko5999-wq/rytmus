# Rytmus — Android aplikace pro čtyři denní závazky

**Hotový zdrojový projekt pro Android Studio.** Vytvořen jako offline aplikace bez účtu, reklam, analytiky a připojení k internetu. Zdroj obsahuje nativní Android upozornění a uživatelské rozhraní v integrovaném WebView (HTML/CSS/JavaScript).

## Co umí

- Čtyři návyky každý den: **osobní modlitba**, **běh nebo procházka**, **studium**, **cvičení**.
- Klepnutím je označíš jako splněné nebo zrušíš označení.
- Kalendář je barevný podle skóre **0/4 červená, 1/4 oranžová, 2/4 žlutá, 3/4 světle zelená, 4/4 zelená**. Dny před prvním použitím jsou neutrálně šedé.
- Můžeš rozkliknout a upravovat minulé dny, navigovat měsíce.
- Aktuální průběh dne, série dokončených dnů, úspěšnost za posledních sedm dní.
- Připomínka konkrétně vypíše **nesplněné úkoly** a automaticky se každý další den obnoví.
- Vše se ukládá **pouze do telefonu** (SharedPreferences). Žádné serverové účty.
- **Oznámení jen při aktivním, odemčeném telefonu**, pokud **nemáš splněné všechny čtyři úkoly**. Po samotném otevření aplikace upozornění pokračují; vypnou se až po 4/4. Každý nový den se možnost připomínání obnoví.
- Režim běžný: nenáročná kontrola maximálně přibližně každých 15 minut, Android ji může odložit. Neprobouzí displej a při vypnuté obrazovce nevydává upozornění.
- Režim přesný: dobrovolně povolená **služba přístupnosti** zachytí systémový signál odemčení telefonu. Při každém dalším odemčení připomene zbývající úkoly, dokud všechny čtyři denní návyky neoznačíš jako splněné. Interval 5, 10 či 20 minut se vztahuje jen na doplňkové kontroly během používání telefonu. **Nečte obsah obrazovky, texty ani hesla**. Žádná data se nikam neposílají.

## Jak z projektu vyrobit APK na Windows

### Možnost A: Android Studio

1. Nainstaluj **Android Studio** z [developer.android.com/studio](https://developer.android.com/studio).
2. Otevři Android Studio → **Open** → vyber rozbalenou složku `RytmusAndroid` (tu s `settings.gradle`).
3. Pokud se studio ptá na Gradle distribuci, použij **Gradle 8.9**. Projekt neobsahuje předem zabalenou distribuci ani oficiální Gradle wrapper. Potřebné Android SDK a Gradle plugin se stáhnou při synchronizaci (vyžadují internet).
4. Ověř, že je nainstalováno **Android SDK API 35** (Settings → SDK Manager).
5. V horním menu zvol **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. Vygenerované APK najdeš po úspěšném sestavení v `app/build/outputs/apk/debug/app-debug.apk`. Přenes ho do Android telefonu a nainstaluj. Android může požádat o povolení instalace z tohoto zdroje.

### Možnost B: jeden skript

Na Windows po instalaci Android Studio a SDK spusť soubor `SESTAVIT_APK_WINDOWS.bat`. Skript stáhne oficiální Gradle 8.9 a zavolá `assembleDebug`. Instalace SDK API 35 může být předem potřeba.

> **Pozor:** V tomto prostředí nebylo dostupné Android SDK ani Gradle a nelze tedy přiložit sestavený a na telefonu ověřený soubor APK. Připraveny jsou zdrojové soubory a funkční prohlížečová ukázka; reálný build je nutné udělat v Android Studiu.

## První spuštění a připomínání

1. Otevři Rytmus a povol **oznámení**.
2. Dole rozbal **Chytré připomínky** a zkontroluj, že je připomínání zapnuto.
3. Volitelně zvol **Zapnout přesné připomínání**. V nastavení Androidu najdi **Rytmus – připomínání při používání telefonu** a službu aktivuj. Tato citlivější systémová volba je dobrovolná; Rytmus nikdy neshromažďuje obsah obrazovky.
4. Na některých telefonech s Androidem 13+ bude po instalaci APK mimo Obchod Play nutné otevřít **Informace o aplikaci → ⋮ → Povolit omezená nastavení** ještě před zapnutím služby přístupnosti. Neaktivuj přístupnost pro aplikace, kterým nedůvěřuješ.
5. Tlačítkem **Vyzkoušet oznámení** si můžeš ověřit, že jsou oznámení povolena. Otevření aplikace připomínání nevypne. Vypne se až po označení všech čtyř denních úkolů jako hotových.

### Proč nejsou připomínky vždy na sekundu přesné?

Android u běžných aplikací omezuje akce na pozadí. Bez trvale aktivní služby nebo speciálního systémového oprávnění nelze garantovat notifikaci na *každé* odemčení. Proto obsahuje aplikace běžný úsporný režim a volitelný přesnější režim. Výrobce telefonu může služby kvůli šetření baterie omezit, včetně zpomalení periodických kontrol.

## Kontrola a soukromí

- Zdrojové soubory: `app/src/main/java/cz/rytmus/app/`.
- Design a logika kalendáře: `app/src/main/assets/index.html`.
- Oprávnění: `POST_NOTIFICATIONS` a `RECEIVE_BOOT_COMPLETED`; volitelná služba přístupnosti vyžaduje souhlas uživatele v nastavení systému. Aplikace nemá oprávnění k internetu, poloze ani kontaktům.
- Všechny údaje zůstávají pouze v úložišti aplikace. Při odinstalaci můžeš o historii přijít, protože zálohování je zakázáno.
- Android 8.0+ (minSdk 26). Cílové API 35. Java 17, Android Gradle Plugin 8.7.3, Gradle 8.9.

## Ukázka bez instalace

Soubor `ukazka/RYTMUS-ukazka.html` lze otevřít v prohlížeči a vyzkoušet vzhled, odškrtávání i kalendář. Webová ukázka neumí sledovat aktivitu telefonu ani posílat nativní připomínky.
