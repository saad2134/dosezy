# Contributing to Dosezy

Thank you for your interest in contributing to Dosezy! Whether you are reporting a bug, proposing a new feature, writing documentation, or contributing code, your help is warmly welcomed.

---

## 📜 Code of Conduct

All contributors are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). Please report any unacceptable behavior to [reach.saad@outlook.com](mailto:reach.saad@outlook.com).

---

## 🔒 Security Vulnerabilities

If you discover a security vulnerability, please do **not** open a public issue. Instead, follow our [Security Policy](SECURITY.md) and report it directly to [reach.saad@outlook.com](mailto:reach.saad@outlook.com).

---

## 🛠️ How to Contribute

### 1. Reporting Bugs & Suggesting Features
- Search existing [GitHub Issues](https://github.com/saad2134/dosezy/issues) to ensure your topic hasn't already been reported.
- If not, create a new issue providing detailed reproduction steps, device info, or a clear feature specification.

### 2. Developing Code

1. **Fork & Clone:**
   ```bash
   git clone https://github.com/<your-username>/dosezy.git
   cd dosezy
   ```

2. **Create a Feature Branch:**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/your-bug-fix
   ```

3. **Monorepo Structure:**
   - `patient/android/`: Native Android application (Jetpack Compose, Room, Hilt, Kotlin Coroutines).
   - `website/`: Marketing and preview web application (Next.js 16, React 19, TypeScript).
   - `docs/`: Architecture documentation and changelogs.

4. **Testing Your Changes:**
   - **Android:** Ensure `./gradlew assembleDebug assembleRelease` compiles cleanly without warnings or errors.
   - **Website:** Run `npm run lint` and `npm run build` inside `/website`.

5. **Submitting a Pull Request:**
   - Commit with clear, descriptive commit messages.
   - Push your branch: `git push origin feature/your-feature-name`.
   - Open a Pull Request on GitHub against `main`.
   - Describe what your changes do and reference any related issues (e.g., `Fixes #12`).

---

## 💬 Contact & Questions

If you have questions or need guidance before starting on a major contribution, feel free to open a GitHub Discussion or reach out to the project maintainer:

📧 **Saad**: [reach.saad@outlook.com](mailto:reach.saad@outlook.com)
