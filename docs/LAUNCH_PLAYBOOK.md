# Repeatless — Launch Playbook

Step-by-step from today's repo to a live Google Play listing. Companion to
`BUSINESS_PLAN.md` (strategy) and `ANDROID_APP_SPEC.md` (technical readiness).
Check items off in order; ~everything before "Launch week" is doable solo.

---

## 0. Do TODAY (an hour, mostly irreversible-if-you-wait)

- [ ] **Register `repeatless.app`** (~US$12–15/yr — Cloudflare Registrar, Namecheap,
      or Porkbun). Was available by DNS on 2026-07-27; availability changes fast.
      Optional: `repeatless.co.za` for the SA market.
- [ ] **Claim handles**: TikTok, Instagram, YouTube, X, Facebook — `@repeatless`
      (fallback `@repeatless.app` / `@getrepeatless`).
- [ ] **Create the Google Play developer account** ($25 once).
      **Decision:** if Prime Paints is a registered legal entity, create an
      **organization account** (needs a D‑U‑N‑S number — free, takes days–weeks;
      start now) → **exempt from the 12-tester closed-testing rule** and cleaner
      legally. Otherwise personal account → plan for 12 testers × 14 days.
- [ ] Create a support email (e.g. `hello@repeatless.app` once the domain is live;
      until then an alias works). Play requires a public support contact.

---

## 1. Technical pre-launch (repo work)

- [x] Audio-privacy architecture (on-device STT, text-only Gemini) — done.
- [x] Firebase/Smart Home stripped; Free/Pro model — done.
- [x] Branded application ID — `app.repeatless`.
- [ ] **Integrate Google Play Billing** (one-time product `pro_unlock`,
      "Restore purchases", replace the local tier toggle). *Blocking for revenue.*
- [ ] Rename remaining in-app copy ("Soundboard PRO" → "Repeatless PRO", About
      dialog, membership dialog).
- [ ] Auto-Parent hardening: per-device daily match cap (e.g. 200); prominent
      mic-use disclosure dialog on first enable (Play policy).
- [ ] **Restrict the Gemini API key** (Cloud Console → key restrictions: Android
      app package `app.repeatless` + SHA-256 cert where supported; set daily
      quota caps). Roadmap v1.1: thin proxy (Cloudflare Worker free tier).
- [ ] Build pass in Android Studio; fix compile issues; `bundleRelease` with the
      upload keystore (see `android/README.md`).
- [ ] Test matrix: API 24 / 30 / latest; no-Google-account device; STT-unavailable
      device; Gemini key absent (heuristic fallback).

## 2. Play Console setup

- [ ] App created as **Repeatless**, category *Parenting* (House & Home fallback:
      no — Parenting exists under Apps → Lifestyle/Parenting), free app + IAP.
- [ ] **Content rating questionnaire** → utility app, no UGC exposure → expect
      Everyone / PEGI 3.
- [ ] **NOT enrolled in "Designed for Families"** — the app targets adults
      (parents). Target audience: 18+.
- [ ] **Data Safety form** (must match reality):
      - Data collected: none stored off-device.
      - Data **shared**: "App activity → other app-generated content" =
        speech **transcripts (text)** + clip labels, shared with Google
        (Gemini API), purpose = app functionality, **optional** (only when
        Auto-Parent is enabled), not linked to identity (no accounts).
      - **Audio: not collected, not shared.** Recordings remain on device.
      - Data encrypted in transit: yes (HTTPS). Deletable: uninstall / in-app
        delete; no server copies.
- [ ] **Privacy policy URL**: `https://primepaints.github.io/Session1/privacy.html`
      (page ships in this repo — enable GitHub Pages; later move to
      `https://repeatless.app/privacy`).
- [ ] App access notes for reviewers: "Auto-Parent requires mic permission; demo
      with the in-app Test Simulator if no audio environment."

## 3. Store listing (ready-to-paste copy)

**Title (≤30 chars):** `Repeatless: Parent Soundboard` *(29)*

**Short description (≤80):**
`Record it once, tap it forever. Your own voice handles the repeating for you.` *(78)*

**Full description:**

> **Stop sounding like a broken record.**
>
> You say the same things every day. "Brush your teeth." "Stop fighting."
> "Eat your veggies." Repeatless lets you say them ONCE — in your own voice —
> and replay them with a tap.
>
> 🎙️ **Record once.** Your real voice, not a robot.
> 🔊 **Tap to play.** Big colourful buttons, built for one hand.
> 🏠 **Home-screen widget.** Fire off "Dinner time!" without opening the app.
> 🧠 **Auto-Parent AI (Pro).** Turn on listening mode and Repeatless hears the
> whining, matches it, and answers with *your* clip — while you drink your
> coffee. Speech is transcribed on your device; **your recordings and your
> kids' voices never leave your phone.** Only text is used for AI matching.
> 🎨 Boards for every routine: mornings, bedtime, car rides, homework.
> 💾 Backup to a file you own. No account. No ads. No subscription.
>
> **Free:** full soundboard, 2 boards, widget, backup.
> **Pro (one-time purchase):** unlimited boards & clips, Auto-Parent AI,
> clip trimming, all themes.
>
> Built by a parent who got tired of repeating himself. Say it once. Tap it
> forever.

**ASO keyword targets** (weave into description updates, test in title later):
parent soundboard · record your own voice · kids routine · stop repeating
yourself · bedtime routine · brush teeth reminder · toddler instructions ·
mom button · dad button · talking buttons

**Screenshots (6, phone; captions on-image):**
1. Board grid — "Your greatest hits. One tap away."
2. Recording sheet — "Record it once, in YOUR voice."
3. Auto-Parent listening — "It hears the whining. It answers. You don't."
4. Privacy card — "Recordings never leave your phone."
5. Widget on home screen — "Nag from the lock screen."
6. Boards/themes — "Mornings. Bedtime. Car rides."

Feature graphic: dark console background, big coral pad reading **"EAT YOUR
VEGGIES"**, finger mid-tap, tagline top-left.

## 4. Closed testing (Path A — personal account)

- [ ] Create closed track, upload AAB, add tester email list (Google Group easiest).
- [ ] Recruit **15–20 testers** (buffer above the 12 minimum; they must stay
      opted-in **14 consecutive days**): family/friends WhatsApp, parent groups,
      r/AndroidClosedTesting (tester-swap), local parenting Facebook groups.
- [ ] Recruitment message (paste-ready):
      > I built an app that lets you record yourself saying "brush your teeth"
      > once and then just press a button forever after. I need 15 parents to
      > beta test it for 2 weeks (Google requires it). You get the Pro version
      > free for life. Reply and I'll send the link. 🎙️
- [ ] During the 14 days: fix crashes fast, collect quotes/testimonials for the
      listing, convert testers into launch-day 5★ reviewers.
- [ ] Apply for production access when eligible; answer Google's questionnaire
      honestly (they ask about testing learnings).

## 5. Marketing engine (start during testing, not after)

**Channel: short-form video (TikTok + Reels + Shorts). Nothing else until this works.**

Week-by-week starter calendar (2–3 posts/week):

| Week | Content |
|---|---|
| 1 | Skit: kid whines about broccoli → phone answers in mom's voice. Founder clip: "I got tired of repeating myself, so I automated me." |
| 2 | "POV: your mom recorded herself" (kid POV, comedic dread). UGC bait: **"What would YOUR button say? 👇"** |
| 3 | Demo: widget spam-tap "DINNER TIME" through the house. Duet/stitch the best week-2 comments into buttons, live. |
| 4 | "Rating my wife's soundboard buttons." Behind-the-scenes: Auto-Parent catching a whine in real time. |

Rules: hook in the first 1.5s (always the *voice firing*), captions on, reply to
every comment with a button-ified version, end-card "Repeatless — free on Google
Play." Track link CTR with Play's UTM referrer.

**Creator seeding:** 10–20 parenting micro-creators (5k–100k), DM template:
> Made a free app that plays your own voice at your kids so you don't have to
> repeat yourself. Want a lifetime Pro code to mess around with? No obligation —
> if it makes you laugh, post it.

**Launch-week extras:** Product Hunt (Tue/Wed), r/SideProject + r/Parenting
(story-first, not ad-first), SA tech/parenting press ("SA dad automates his own
nagging"), personal LinkedIn/Facebook founder post.

## 6. Landing page (once domain is live)

Single page at `repeatless.app`: logo; H1 **"Say it once. Tap it forever."**;
15-sec demo video; three bullets (own voice / AI answers for you / nothing leaves
your phone); Google Play badge; email capture ("iPhone? Join the iOS waitlist" —
this measures iOS demand for free); footer → privacy policy. Build with any
static host (GitHub Pages/Cloudflare Pages, $0).

## 7. Post-launch rhythm (first 90 days)

- Reply to **every** Play review (algorithmic + social proof).
- Weekly: check vitals (ANR/crash), Auto-Parent API spend vs cap, conversion
  funnel (install → ≥3 pads recorded → Pro).
- v1.1 (weeks 2–6): daily match cap, most-requested feature, in-app copy rebrand
  pass, pricing experiment ($4.99 vs $6.99).
- Month 3 gate: ≥5k installs & ≥2.5% conversion → invest (iOS eval, paid UA
  tests, localization). Below → iterate content formats, not the product.

## 8. Legal/admin checklist

- [ ] Formal trademark screen for "Repeatless" (ZA CIPC + WIPO/USPTO search;
      classes 9 & 42) before paid brand assets.
- [ ] Decide entity: run through Prime Paints vs personal vs new entity (tax +
      liability; org Play account requires the entity anyway on Path B).
- [ ] Keep the privacy policy, Data Safety form, and in-app disclosure in sync
      with any future data-flow change — they must never drift from the code.
