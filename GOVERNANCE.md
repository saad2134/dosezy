# Governance

## Overview

Dosezy is a free and open-source project focused on private, accessible, reliable, and user-controlled medication management.

The project is designed around a local-first philosophy: the core Dosezy experience should remain useful without an internet connection, without requiring an account, and without requiring users to trust a cloud service with their data.

Dosezy may eventually provide optional cloud and caregiver services through **Dosezy Cloud** and **Dosezy Care**, but these services are intended to enhance the core application rather than make it dependent on a hosted service.

This document describes how Dosezy is governed, how decisions are made, and the principles that guide the project's development.

---

## Project Maintainer

### Saad

**GitHub:** https://github.com/saad2134
**Repository:** https://github.com/saad2134/dosezy
**Contact:** [reach.saad@outlook.com](mailto:reach.saad@outlook.com)

Saad is the current primary maintainer and project lead for Dosezy.

The maintainer is responsible for:

* Project direction and long-term architecture
* Reviewing and merging contributions
* Managing releases
* Maintaining project infrastructure
* Reviewing security reports
* Maintaining community standards
* Coordinating major architectural changes
* Protecting the project's privacy, accessibility, and reliability goals
* Making final decisions when consensus cannot be reached

As Dosezy grows, additional long-term maintainers may be recognized based on sustained contributions, technical judgment, project knowledge, and demonstrated commitment to the project's principles.

---

# Project Principles

Dosezy development is guided by the following principles.

## 1. Free and Open Source

The core Dosezy experience should remain free and open source.

Users should be able to:

* Inspect the source code
* Build the software themselves
* Fork the project
* Contribute improvements
* Audit how the application works
* Use the core application without being forced into a paid service

Dosezy is currently released under the MIT License.

---

## 2. Local-First

Dosezy should work locally whenever possible.

Basic medication management must not depend on a cloud service.

The core experience should remain functional when:

* The device is offline
* A cloud service is unavailable
* The user does not have an account
* The user chooses not to use cloud services

Cloud functionality should supplement local functionality rather than replace it.

---

## 3. Privacy by Default

Medication information can be highly sensitive.

Dosezy should therefore minimize unnecessary data collection and avoid collecting information that is not required for a specific product feature.

The project should avoid unnecessary collection of:

* Advertising identifiers
* Precise location
* Contacts
* Unrelated health information
* Other personal information that is not necessary for Dosezy functionality

When cloud functionality is introduced, security and privacy must be considered as architectural requirements rather than features added later.

---

## 4. Cloud Must Never Hold Dosezy Hostage

This is a core project principle:

> **Dosezy Cloud must enhance Dosezy, never hold Dosezy hostage.**

This means:

* Cloud unavailable → Dosezy should continue to work locally.
* User does not want an account → core local functionality should remain available.
* User cancels a paid service → their local medication functionality should remain available.
* User deletes their account → local functionality should not unnecessarily disappear.
* Cloud service is discontinued → users should have a reasonable way to export their data.
* Users do not trust the official cloud → the architecture should allow local-first use and, where practical, self-hosting.

Cloud services exist to provide additional value such as backup, synchronization, multi-device access, and caregiver functionality.

---

# 5. Accessibility Is a Core Requirement

Accessibility is not intended to be a premium feature or an optional enhancement.

Dosezy should prioritize an experience that is usable by people with different abilities and levels of technical familiarity, including elderly users.

Contributors should consider:

* Readability
* Clear typography
* Touch target sizes
* Contrast
* Navigation simplicity
* Understandable language
* Notification clarity
* Reduced interaction complexity
* Screen-reader compatibility
* Assistive technology
* Elderly-friendly workflows

Accessibility regressions should be treated as meaningful product regressions.

---

# 6. No Advertising in the Core Experience

Dosezy should not rely on advertising as a core monetization strategy.

Medication reminders and health-related workflows should not be interrupted by advertisements.

The project prioritizes trust and usability over advertising revenue.

---

# 7. Basic Medication Management Should Remain Free

The core functions of Dosezy should not be placed behind a subscription.

This includes the fundamental ability to:

* Manage medications
* Create schedules
* Receive basic reminders
* View medication history
* Use the application offline
* Use accessibility features
* Export personal data
* Manage basic profiles

Optional paid services may provide additional infrastructure or connected-care functionality.

---

# 8. Optional Cloud and Caregiver Services

The project may provide an optional commercial layer through services such as:

**Dosezy Cloud**

Potential capabilities include:

* Backup
* Synchronization
* Multi-device support
* Account-based services

**Dosezy Care**

Potential capabilities include:

* Caregiver invitations
* Family coordination
* Adherence sharing
* Caregiver dashboards
* Remote medication-management support

These services should add value without making the core local application dependent upon them.

---

# 9. Open-Source Infrastructure

Where practical and appropriate, Dosezy's server-side software and API architecture should remain open source alongside the clients.

The intended architecture may include:

```text
Dosezy
├── apps/
│   ├── android/
│   └── ios/
├── server/
├── api/
│   └── openapi.yaml
├── web/
├── docs/
└── scripts/
```

The official Dosezy Cloud may operate hosted instances of the same open-source infrastructure.

Where technically and operationally practical, the project may also support self-hosted deployments.

Self-hosting is intended to strengthen user control, transparency, privacy, and long-term resilience.

---

# 10. API and Data Contracts

As Dosezy expands across Android, iOS, web, and server components, shared contracts should be treated as first-class project artifacts.

API changes should update the corresponding OpenAPI specification in the same pull request.

For example:

```text
Feature:
Caregiver adherence endpoint

Expected changes:

✓ api/openapi.yaml
✓ server implementation
✓ Android implementation
✓ iOS implementation
✓ Web implementation
✓ Tests
```

The goal is to prevent clients and server implementations from drifting away from the agreed API contract.

The API specification should describe the contract between components rather than force every platform to share the same implementation.

---

# 11. Platform Architecture

Dosezy may support multiple platforms over time.

The project favors platform-appropriate implementations:

* Android → Kotlin + Jetpack Compose
* iOS → Swift + SwiftUI
* Web → appropriate web technologies
* Server → an appropriate backend stack

Shared behavior should primarily be established through:

* Domain concepts
* Data models
* API contracts
* Synchronization rules
* Product specifications
* Documentation
* Design principles

Platform-specific implementations should be allowed to follow the conventions of their respective ecosystems.

---

# Decision Making

## Routine Changes

Routine changes can normally be handled through standard pull requests.

Examples include:

* Bug fixes
* Documentation improvements
* Tests
* Accessibility improvements
* Localization
* Small UI improvements
* Dependency updates
* Refactoring
* Performance improvements

The maintainer may approve and merge these changes when they meet project standards.

---

## Significant Changes

Significant changes should normally be discussed before implementation when they affect:

* Application architecture
* Data models
* Synchronization
* Privacy
* Security
* Medication behavior
* Accessibility
* Public APIs
* Licensing
* Cloud architecture
* Authentication
* Caregiver permissions
* Major product direction

Contributors are encouraged to open a GitHub Discussion or issue before implementing a substantial architectural change.

---

## Breaking Changes

Breaking changes require additional consideration.

A breaking change should explain:

1. What is changing.
2. Why the change is necessary.
3. Who is affected.
4. What migration path exists.
5. How compatibility will be handled.

Breaking changes should not be introduced merely for convenience when a backwards-compatible approach is reasonably achievable.

---

# Pull Requests

Pull requests are the primary mechanism for proposing changes to the project.

Contributors should:

* Clearly describe the change.
* Explain why it is needed.
* Keep unrelated changes out of the PR.
* Add or update tests where appropriate.
* Consider accessibility.
* Consider privacy and security.
* Update documentation where necessary.
* Update API specifications when API behavior changes.
* Avoid committing secrets or credentials.

The maintainer may request revisions, approve, reject, or defer a pull request.

Approval does not guarantee immediate merging.

---

# Security

Security issues must not be disclosed through public GitHub Issues or Discussions.

Security vulnerabilities should be reported according to [`SECURITY.md`](./SECURITY.md).

Because Dosezy may handle medication-related information, security considerations receive particular priority for:

* Authentication
* Authorization
* Data storage
* Synchronization
* Cloud infrastructure
* Caregiver access
* API endpoints
* Data export
* Encryption
* Secrets management

Production credentials, signing keys, API secrets, database passwords, encryption keys, and similar sensitive information must never be committed to the repository.

---

# Data and Privacy Architecture

The project should follow the principle of collecting and processing only what is necessary.

For cloud-enabled functionality, the architecture should consider:

```text
Device
   │
   │ HTTPS
   ▼
Dosezy API
   │
   ▼
Encrypted storage
```

Sensitive information should receive appropriate protection both in transit and at rest.

Cloud architecture should not be introduced merely because modern applications are expected to have cloud functionality.

A cloud feature should have a clear user benefit.

---

# Roadmap and Releases

The project roadmap may evolve as real user needs become clearer.

Potential long-term development stages include:

```text
Local-first
    │
    ├── Android
    ├── Medication management
    ├── Reminders
    ├── History
    ├── Profiles
    ├── Export
    └── Accessibility
            │
            ▼
Architecture
    │
    ├── Domain layer
    ├── Repository layer
    ├── Sync abstraction
    └── API specification
            │
            ▼
Dosezy Cloud
    │
    ├── Accounts
    ├── Backup
    ├── Synchronization
    └── Multi-device
            │
            ▼
iOS
    │
    ├── SwiftUI
    ├── Local-first
    └── Cloud synchronization
            │
            ▼
Dosezy Care
    │
    ├── Caregiver invitations
    ├── Permissions
    ├── Adherence sharing
    └── Caregiver dashboard
            │
            ▼
Web
    │
    └── Caregiver dashboard
```

These stages represent direction rather than guaranteed release dates.

The project should avoid promising dates for future functionality until the relevant work has been validated and scheduled.

---

# Monorepo

Dosezy currently favors a monorepo approach as the project expands.

A monorepo allows related components to evolve together while keeping:

* API contracts
* Client applications
* Server implementation
* Documentation
* Tests
* Shared specifications

in one project.

Repositories may be split in the future if there is a demonstrated need, such as:

* Independent teams
* Independent release cycles
* Repository size becoming problematic
* Different access requirements
* A server becoming a substantially independent product
* External organizations contributing independently

Repository separation should be a deliberate decision rather than an assumption made for architectural fashion.

---

# Maintainer Responsibilities

The maintainer should protect the project's long-term health by considering:

* User safety
* Privacy
* Accessibility
* Reliability
* Security
* Maintainability
* Open-source sustainability
* Community health

The maintainer may reject technically valid contributions when they conflict with these principles or with the long-term direction of Dosezy.

---

# Becoming a Maintainer

Maintainer status is based on sustained contribution and demonstrated responsibility rather than a single contribution.

Potential maintainers should demonstrate:

* Consistent high-quality contributions
* Understanding of Dosezy's architecture
* Respect for the project's principles
* Good communication
* Security awareness
* Ability to review and maintain code
* Commitment to the project's open-source community

Maintainer appointments are ultimately made by the existing maintainer(s).

As the project grows, this process may be formalized further.

---

# Community Conduct

All participants are expected to follow [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md).

Contributors should communicate respectfully and constructively.

Harassment, discrimination, malicious behavior, deliberate disruption, and abuse of project infrastructure are not acceptable.

---

# Changes to This Governance Document

This document may evolve as Dosezy grows.

Proposed governance changes should be submitted through a GitHub issue, discussion, or pull request.

The current maintainer has final authority over governance while Dosezy remains under its current maintainer-led structure.

If Dosezy develops a larger maintainer team, foundation, company, or community governance structure, this document should be updated to reflect that new structure.

---

# Guiding Principles

When a decision is unclear, the project should generally prioritize:

1. **User safety**
2. **Privacy**
3. **Accessibility**
4. **Reliability**
5. **User control**
6. **Local-first functionality**
7. **Open-source transparency**
8. **Security**
9. **Simplicity**
10. **Maintainability**

Above all:

> **Dosezy Cloud must enhance Dosezy, never hold Dosezy hostage.**

The project should remain useful to someone who simply wants a private, accessible, offline medication-management application, while providing an optional path toward connected care for users who need it.
