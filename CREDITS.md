# Open Source Credits

This project uses the following open source libraries and components.
We thank all the developers and contributors for their wonderful work.

---

## Core Dependencies

### AndroidX Libraries

| Library | Group | License |
|:--|:--|:--|
| AndroidX Core KTX | `androidx.core:core-ktx:1.13.1` | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| AndroidX Activity Compose | `androidx.activity:activity-compose:1.9.2` | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| AndroidX Compose BOM | `androidx.compose:compose-bom:2024.09.03` | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| AndroidX Material3 | `androidx.compose.material3:material3:1.3.0` | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| AndroidX Preference KTX | `androidx.preference:preference-ktx:1.2.1` | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |

**Compose UI Tooling (Debug)**
- `androidx.compose.ui:ui-tooling` — [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- `androidx.compose.ui:ui-test-manifest` — [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)

### Google Libraries

| Library | Group | License |
|:--|:--|:--|
| Google Material Components | `com.google.android.material:material:1.12.0` | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |

### Third-Party Libraries

| Library | Group | License | Homepage |
|:--|:--|:--|:--|
| SimpleMenu Preference | `dev.rikka.rikkax.preference:simplemenu-preference:1.0.3` | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) | [RikkaW/MaterialPreference](https://github.com/RikkaW/MaterialPreference) |

> **Note**: The upstream repository is deprecated. This dependency is retained for legacy compatibility only.
> 
> Now this app uses Google Material Components instead, with the equal function.

---

## Test Dependencies

| Library | Group | License |
|:--|:--|:--|
| JUnit | `junit:junit:4.13.2` | [EPL 1.0](https://www.eclipse.org/legal/epl-v10.html) |
| AndroidX JUnit | `androidx.test.ext:junit:1.2.1` | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| AndroidX Espresso Core | `androidx.test.espresso:espresso-core:3.6.1` | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| Compose UI Test JUnit4 | `androidx.compose.ui:ui-test-junit4` | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |

---

## Build Tools & Plugins

| Plugin | ID | License |
|:--|:--|:--|
| Android Gradle Plugin | `com.android.application` v8.6.1 | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| Kotlin Android Plugin | `org.jetbrains.kotlin.android` v1.9.0 | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |

---

## Font Resources

> **Note**: This app uses system fonts or user-provided variable fonts.
> No font files are bundled with this application.

---

## Icons & Graphics

The app icon foreground ("VF" letterform) is derived from
[Lawnicons](https://github.com/LawnchairLauncher/lawnicons)
by the [Lawnchair](https://github.com/LawnchairLauncher) team,
licensed under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).
Modifications include stroke weight adjustment and adaptive icon integration.

---

## License Summary

Most dependencies in this project are licensed under the **Apache License 2.0**,
which is compatible with the project's main license (**GNU GPL v3**).

- **Apache 2.0**: AndroidX, Google Material, RikkaX, Lawnicons
- **EPL 1.0**: JUnit (test only)
- **GPL v3**: This project itself

---

## Acknowledgements

Special thanks to:
- [AndroidX Team](https://developer.android.com/jetpack/androidx)
- [Material Design Team](https://m3.material.io/)
- [Lawnchair Team](https://github.com/LawnchairLauncher) for the Lawnicons icon set
- [RikkaW](https://github.com/RikkaW) for SimpleMenu Preference
- [Qwen Team](https://qwen.ai) for the powerful LLM service conducting code design and code implementation
- [DeepSeek Team](https://www.deepseek.com) for the high-efficiency LLM service cleaning up the code
- [WordlessEcho](https://github.com/WordlessEcho) for the original project foundation
- [Google Fonts](https://fonts.google.com/specimen/Google+Sans+Flex/license) for the Google Sans Flex in the v4 app demo video

> Google Fonts also provides the high-quality Roboto, NotoSansCJK, and NotoSerifCJK fonts seen in the video, and we would like to thank them for that.

> The variable NotoSerifCJK font ttc file in the video comes from [this patch](https://github.com/simonsmh/notocjk). 

> The Chinese fonts used in the v4 app demo video are [HarmonyOS Sans](https://developer.huawei.com/consumer/cn/design/resource/#aca70492-8edf-4ae8-b789-74ed7daa2796) and [OPPO Sans](https://www.coloros.com/article/A00000050/), which are free for commercial use.

- All open source contributors (including translators)

---

*Last updated: 2026 Summer*