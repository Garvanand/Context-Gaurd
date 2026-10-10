# ContextGuard Brand Identity & System Guidelines
## Creative Direction: Spectral Signal • The Aperture Signal

---

## 1. Executive Summary & Brand Philosophy

ContextGuard represents a paradigm shift in autonomous multimodal safety: **The risk of a digital artifact cannot be judged from the artifact alone; rather, the intended action and surrounding context determine the appropriate safety intervention.**

To communicate this thesis, the brand identity departs from conventional cybersecurity tropes:
* **NO Security Shields:** Antivirus and firewall software rely on static perimeter defense shields. ContextGuard is a dynamic contextual observer, not a padlock.
* **NO AI Sparkles / Magic Stars:** Four-pointed sparkle glyphs suggest unpredictable generative hallucination. ContextGuard is an engineered, deterministic, action-aware safety system.
* **The Aperture Signal Metaphor:** A signal passing through uncertainty. The mark evokes contextual awareness, focused focal attention, and responsible intervention before a consequential action.

```
                  ┌────────────────────────────────────────┐
                  │          THE APERTURE SIGNAL           │
                  │                                        │
                  │     • Incomplete Geometric Aperture    │
                  │     • Two Offset Signal Contours       │
                  │     • Engineered Action Discontinuity  │
                  │     • Concentric Negative Channel      │
                  └────────────────────────────────────────┘
```

---

## 2. Direction Exploration & Selection Rationale

Three distinct design directions were explored internally before arriving at the final identity:

| Direction | Concept Metaphor | Structural Description | Evaluation & Selection Decision |
| :--- | :--- | :--- | :--- |
| **Direction 1: The Dual-Phase Lens** | Optical interference of two probability waves | Two intersecting elliptical crescents with an off-axis focal ring. | **Rejected:** Rendered poorly below 24dp due to razor-thin apex intersections. Looked too academic / optical rather than an active safety guardian. |
| **Direction 2: The Offset Aperture Horizon** | **Contextual reticle with action-gating pause** | **270° outer aperture contour (Wing Alpha) + 240° inner offset contour (Wing Beta) + 45° central discontinuity slit + observer anchor.** | **SELECTED FINAL IDENTITY:** Exceptional silhouette recognition at all sizes (16dp to 108dp). Pure circular arcs provide mathematical balance; intentional discontinuity communicates the pause before action. |
| **Direction 3: The Prismatic Reticle** | Segmented polar coordinate bounding box | Four segmented corner brackets framing a crosshair dot with spectral gradient. | **Rejected:** Felt excessively militaristic and tactical (like a weapons targeting reticle), directly conflicting with ContextGuard's calm, trustworthy, human-centric positioning. |

---

## 3. Geometric Construction & Mathematical Grid

The Aperture Signal is constructed on a standardized **100 × 100 unit coordinate system** with origin `(0, 0)` at the top-left and geometric center at `(50, 50)`.

```
                    (50, 0)
                      │
            .─── 90° Intake Gap ───.
         .-'                        '-.  Wing Alpha (R_out=38, R_in=28)
       .'     .───────.                '.
      /     .'         '.                \
     │     /    (50,50)  \                │
(0,50)────│──── 45° Slit ──│──────────────(100,50)
     │     \    Anchor • /                │
      \     '.         .'                /  Wing Beta (R_out=23, R_in=15)
       '.     '───────'                .'
         '-.                        .-'
            '──────────────────────'
                      │
                   (50, 100)
```

### Component Coordinates & Dimensions:

1. **Wing Alpha (Outer Signal Contour):**
   * Outer Radius: $R_1 = 38\text{ units}$
   * Inner Radius: $R_2 = 28\text{ units}$
   * Contour Thickness: $10\text{ units}$
   * Angular Span: $270^\circ$ clockwise arc starting at angle $-10^\circ$ and ending at $260^\circ$.
   * Intake Aperture: An engineered $90^\circ$ gap in the upper-right quadrant ($260^\circ \to 350^\circ$) representing contextual data ingestion.
   * Path Data: `M 87.42 43.40 A 38 38 0 1 1 43.40 12.58 L 45.14 22.43 A 28 28 0 1 0 77.57 45.14 Z`

2. **Wing Beta (Inner Offset Signal Contour):**
   * Outer Radius: $R_3 = 23\text{ units}$
   * Inner Radius: $R_4 = 15\text{ units}$
   * Contour Thickness: $8\text{ units}$
   * Concentric Channel: Uniform $5\text{ unit}$ negative space buffer ($28 - 23 = 5$) separating Wing Alpha and Wing Beta.
   * Angular Span: $240^\circ$ arc offset in phase, starting at $50^\circ$ and ending at $290^\circ$.
   * Path Data: `M 64.78 67.62 A 23 23 0 1 1 57.87 28.39 L 55.13 35.90 A 15 15 0 1 0 59.64 61.49 Z`

3. **Central Aperture Focal Core (Action-Gating Core):**
   * Radius: $R_{core} = 9\text{ units}$
   * Geometric Quadrant: $90^\circ$ sector centered at `(50, 50)` spanning $135^\circ$ to $225^\circ$.
   * Discontinuity Slit: An intentional $45^\circ$ diagonal clearance cutting across the reticle center, embodying the deterministic policy pause ($\rho = s \cdot (1 + \lambda r)$).
   * Path Data: `M 42.72 55.44 A 9 9 0 0 1 55.44 42.72 Z`

4. **Observer Anchor Point:**
   * Precision circular beacon at `(55.0, 55.0)` with radius $R_{anchor} = 2.8\text{ units}$.
   * Represents the user as the ultimate sovereign observer in pre-action verification.

---

## 4. Color Palette & Systematic Tokens

| Token Name | Hex Code | Role in Brand Mark | Cultural & Functional Rationale |
| :--- | :--- | :--- | :--- |
| **Electric Violet** | `#8B70FF` | Primary Wing Alpha | High-frequency signal spectrum; intelligence and calm assurance. |
| **Ion Cyan** | `#45E4FF` | Secondary Wing Beta | Focused contextual beam; clarity of reasoning and data flow. |
| **Signal Lime** | `#D8FF63` | Central Focal Core | Consequential action trigger; deterministic safety threshold. |
| **Soft White** | `#F4F6FF` | Observer Anchor & Typography | High-contrast readability, clinical precision. |
| **Midnight Slate** | `#080A12` | Dark Background Canvas | Deep cinematic substrate; eliminates glare and eye fatigue. |

### Color Variants:
* **Spectral Full Color (Default):** Electric Violet + Ion Cyan + Signal Lime + Soft White on Midnight Slate.
* **Monochrome Dark:** Pure `#F4F6FF` (with 82% alpha on Wing Beta) for dark backgrounds and status bars.
* **Monochrome Light:** Pure `#080A12` (with 82% alpha on Wing Beta) for printed documentation or high-brightness media.
* **Android Notification Silhouette:** Solid `#FFFFFFFF` alpha-mask, compliant with Android platform notification tinting standards.

---

## 5. Scaling & Clear Space Rules

```
               ┌──────────────────────────────┐
               │              X               │
               │   ┌──────────────────────┐   │
               │ X │   [APERTURE SIGNAL]  │ X │
               │   └──────────────────────┘   │
               │              X               │
               └──────────────────────────────┘
                       X = 0.25 × Height
```

* **Clear Space Rule:** Minimum clear space of $X = 0.25 \times \text{height}$ must surround the mark on all four sides. No typography, bounding strokes, or decorative lines may encroach on this zone.
* **Minimum Digital Sizes:**
  * **16 × 16 dp / px:** Minimum size for browser tab favicons, status beacons, and list item indicators.
  * **24 × 24 dp:** Standard Android system notification and compact toolbar icons.
  * **36 × 36 dp:** Standard TopAppBar and navigation header badges.
  * **108 × 108 dp:** Android Adaptive Launcher Icon (with 72dp safe inner circular boundary, mark scaled to 54dp).

---

## 6. Android Adaptive Icon & Launcher Architecture

Android 8.0+ (API 26+) adaptive icons require a **108 × 108 dp** canvas where launchers apply diverse masks (Circle, Squircle, Rounded Square, Teardrop) inside an inner **72 dp diameter safe zone**.

```
              108 dp Canvas
     ┌──────────────────────────────┐
     │      Outer 18dp Bleed        │
     │      ┌────────────────┐      │
     │      │  72dp Safe Zone│      │
     │      │   ┌────────┐   │      │
     │      │   │54dp Sym│   │      │
     │      │   └────────┘   │      │
     │      │                │      │
     │      └────────────────┘      │
     │                              │
     └──────────────────────────────┘
```

### Resource Implementation:
* **Background (`ic_launcher_background.xml`):** Deep Midnight Slate (`#080A12`) base with layered subtle concentric rings (`#0E1222`, `#151B30`, `#0B0E19`) providing depth across both light and dark Android home screens.
* **Foreground (`ic_launcher_foreground.xml`):** The Aperture Signal mark scaled to $54\text{ dp}$ (scale factor `0.54`, translate `(27, 27)`). Zero clipping under any OEM launcher mask with $9\text{ dp}$ of breathing space.
* **Mipmaps (`mipmap-anydpi-v26/ic_launcher.xml`):** Linked to adaptive icon background and foreground for 100% vector fidelity across all DPI densities (`mdpi` through `xxxhdpi`).

---

## 7. Distinctive Brand Behavior: Logo-Reveal Animation

To reinforce the engineered, deterministic nature of ContextGuard, a bespoke Compose animation was created: `ApertureSignalLogoReveal`.

### Motion Choreography (Total Duration: 1600 ms):
1. **Stage 1: Signal Ingestion (0 ms – 650 ms)**
   * Wing Alpha sweeps from $0^\circ \to 270^\circ$ with `FastOutSlowInEasing`.
   * Wing Beta sweeps from $0^\circ \to 240^\circ$ starting after a $120\text{ ms}$ phase delay.
2. **Stage 2: Approach Alignment (600 ms – 1100 ms)**
   * Both contours rotate toward equilibrium (Alpha: $+18^\circ \to 0^\circ$, Beta: $-22^\circ \to 0^\circ$).
   * Deliberate mechanical pause: contours approach alignment but **stop intentionally**, preserving the $45^\circ$ discontinuity slit and $90^\circ$ intake gap.
3. **Stage 3: Discontinuity Gate & Core Lock-In (1100 ms – 1600 ms)**
   * Central action-gating focal core and observer anchor appear via an engineered spring settle (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessMedium`).
   * Visual reticle calibration locks the system into operational readiness.

### Interaction Guardrail:
* **Played ONLY during onboarding (`WelcomeScreen`) or deliberate brand introduction.**
* **Never played on every app open or activity resume**, ensuring zero disruption to everyday safety triage.

---

## 8. Complete Asset Inventory

| Asset Name | Target Platform / Role | File Location |
| :--- | :--- | :--- |
| **Aperture Signal Standalone SVG** | Web, Vector Master, Print | `assets/brand/aperture_signal_symbol.svg` |
| **Monochrome Dark Symbol SVG** | Dark Backgrounds, High-Contrast UI | `assets/brand/aperture_signal_symbol_mono_dark.svg` |
| **Monochrome Light Symbol SVG** | Light Media, Print, Documentation | `assets/brand/aperture_signal_symbol_mono_light.svg` |
| **Horizontal Logo with Wordmark SVG** | Web Header, Brand Guidelines, Deck | `assets/brand/aperture_signal_horizontal.svg` |
| **Compact App-Bar Logo SVG** | Navigation Bars, Embedded Tools | `assets/brand/aperture_signal_appbar.svg` |
| **Web Favicon** | Browser Tabs (Supervisor Room) | `supervisor-dashboard/public/favicon.svg` |
| **Android VectorDrawable Symbol** | In-App Compose & Native Views | `android/app/src/main/res/drawable/ic_aperture_signal_symbol.xml` |
| **Android Horizontal VectorDrawable** | Android Headers & Banners | `android/app/src/main/res/drawable/ic_aperture_signal_horizontal.xml` |
| **Android Notification Icon** | Android Status Bar & Notification Tray | `android/app/src/main/res/drawable/ic_notification_contextguard.xml` |
| **Adaptive Launcher Background** | Android Home Screen Substrate | `android/app/src/main/res/drawable/ic_launcher_background.xml` |
| **Adaptive Launcher Foreground** | Android Home Screen Icon Mark | `android/app/src/main/res/drawable/ic_launcher_foreground.xml` |
| **Adaptive Mipmap XMLs** | Launcher Manifest Resolution | `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` |
| **React Logo Component** | Vite / React Supervisor Dashboard | `supervisor-dashboard/src/components/ApertureSignalLogo.tsx` |
| **Compose Logo-Reveal Animation** | Android Onboarding Experience | `android/app/src/main/java/com/contextguard/app/ui/components/ApertureSignalLogoReveal.kt` |

---

## 9. Brand Misuse & Anti-Patterns

To maintain brand integrity, the following manipulations are strictly prohibited:

```
    [X] DO NOT enclose in a generic shield or padlock shape
    [X] DO NOT add 4-pointed AI sparkles or magical starbursts
    [X] DO NOT close the 45° discontinuity slit or 90° aperture gap
    [X] DO NOT skew, stretch, or alter the 1:1 aspect ratio of the symbol
    [X] DO NOT apply heavy outer glows, drop shadows, or Gaussian blurs at small sizes
    [X] DO NOT substitute emojis (🛡️, ✨, 🔒) or icon-library text glyphs
```

---

## 10. Summary

The Aperture Signal establishes ContextGuard as an intelligent, cinematic, high-assurance AI safety product. Every line and gap is grounded in the underlying thesis: bringing meaningful signals into focus and affording users a moment of engineered clarity before taking a consequential action.
