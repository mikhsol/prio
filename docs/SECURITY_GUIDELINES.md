# Prio - Security & Privacy Guidelines

## Security Philosophy

### Core Principles

1. **Privacy by Design**: Security and privacy are built into the architecture, not bolted on
2. **Defense in Depth**: Multiple layers of security at every level
3. **Zero Trust**: Never trust, always verify—even internal services
4. **Minimal Data**: Collect only what's needed, retain only as long as necessary
5. **Transparency**: Users understand what data we have and how it's used

---

## Threat Model

### Assets to Protect

| Asset | Sensitivity | Impact if Compromised |
|-------|-------------|----------------------|
| User credentials | Critical | Account takeover |
| Personal data (contacts, calendar) | High | Privacy violation |
| Conversation history | High | Privacy violation |
| Payment information | Critical | Financial loss |
| AI training data | Medium | Competitive exposure |
| API keys/secrets | Critical | System compromise |

### Threat Actors

| Actor | Motivation | Capability |
|-------|------------|------------|
| Opportunistic hackers | Financial gain | Low-Medium |
| Organized crime | Financial gain | Medium-High |
| Nation states | Espionage | High |
| Malicious insiders | Various | Medium |
| Competitors | Business advantage | Medium |

### Attack Vectors

1. **Mobile app vulnerabilities**: Reverse engineering, tampering
2. **API attacks**: Injection, authentication bypass
3. **Infrastructure**: Misconfiguration, unpatched systems
4. **Social engineering**: Phishing, pretexting
5. **Supply chain**: Compromised dependencies
6. **Insider threats**: Unauthorized data access

---

## Authentication & Authorization

### Authentication

#### Primary Authentication
- Email + Password (minimum requirements below)
- OAuth 2.0 social login (Apple, Google)
- Magic link email authentication

#### Password Requirements
- Minimum 12 characters
- At least 1 uppercase, 1 lowercase, 1 number, 1 special character
- Check against breach databases (HaveIBeenPwned API)
- Argon2id hashing with appropriate parameters:
  ```
  Memory: 64MB
  Iterations: 3
  Parallelism: 4
  Salt: 16 bytes random
  ```

#### Multi-Factor Authentication (MFA)
- TOTP (authenticator apps) - recommended
- SMS (fallback, with security warnings)
- Biometric (device-based, not transmitted)
- Hardware keys (FIDO2/WebAuthn)

#### Session Management
- JWT tokens with short expiry (15 minutes access, 7 days refresh)
- Secure token storage (Keychain/Keystore)
- Token rotation on refresh
- Device binding
- Concurrent session limits (configurable)

### Authorization

#### Role-Based Access Control (RBAC)
```
Roles:
- free_user: Basic features
- premium_user: All features
- admin: Internal tools
- support: Read-only user data access
- developer: API and system access
```

#### Attribute-Based Access Control (ABAC)
- Resource ownership checks
- Organization membership (future)
- Geographic restrictions (compliance)

---

## Data Security

### Encryption

#### At Rest
| Data Type | Encryption | Key Management |
|-----------|------------|----------------|
| Database | AES-256-GCM | AWS KMS / GCP KMS |
| File storage | AES-256-GCM | AWS KMS / GCP KMS |
| Backups | AES-256-GCM | Separate key hierarchy |
| Mobile local storage | iOS Keychain / Android Keystore | Device-bound |

#### In Transit
- TLS 1.3 required for all connections
- Certificate pinning in mobile apps
- Perfect Forward Secrecy (PFS)
- HSTS with preloading

#### Sensitive Field Encryption
Encrypt at application level before database storage:
- Phone numbers
- Physical addresses
- Financial data
- Health information (if any)

### Data Classification

| Classification | Examples | Controls |
|----------------|----------|----------|
| Public | Marketing content | None required |
| Internal | Business metrics | Access controls |
| Confidential | User PII | Encryption, access logging |
| Restricted | Passwords, keys | HSM, need-to-know access |

### Data Retention

| Data Type | Retention Period | Deletion Method |
|-----------|------------------|-----------------|
| Account data | Until deletion requested | Secure wipe |
| Conversation history | 90 days (configurable) | Secure wipe |
| Analytics (anonymized) | 2 years | Aggregation |
| Audit logs | 7 years | Archive, then wipe |
| Marketing preferences | Until opt-out | Immediate |

---

## API Security

### Input Validation
- Validate all inputs on server side
- Use parameterized queries (no string concatenation)
- Sanitize outputs (prevent XSS)
- Implement request size limits
- File upload restrictions (type, size, scanning)

### Rate Limiting
```
Endpoints:
- /auth/login: 5 requests/minute per IP
- /auth/register: 3 requests/minute per IP
- /api/*: 100 requests/minute per user
- /assistant/*: 50 requests/minute per user (free), 500 (premium)
```

### API Authentication
- Bearer tokens (JWT)
- API keys for server-to-server
- OAuth 2.0 for third-party integrations
- Webhook signatures (HMAC-SHA256)

### Security Headers
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
Content-Security-Policy: default-src 'self'; ...
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), microphone=(self), camera=()
```

---

## Mobile Security

### iOS Security

#### App Security
- App Transport Security (ATS) enforced
- Keychain for sensitive storage
- Certificate pinning
- Jailbreak detection (with graceful degradation)
- No sensitive data in UserDefaults
- Clear clipboard on app background

#### Code Security
- Obfuscation for sensitive code paths
- Anti-tampering checks
- Debugger detection
- No hardcoded secrets

### Android Security

#### App Security
- Network Security Config (TLS enforcement)
- EncryptedSharedPreferences
- Android Keystore for secrets
- Root detection (with graceful degradation)
- ProGuard/R8 obfuscation
- SafetyNet/Play Integrity attestation

#### Code Security
- NDK for sensitive operations
- No hardcoded secrets
- Debugger detection
- Tamper detection

### Secure Local Storage

```swift
// iOS - Keychain example
let query: [String: Any] = [
    kSecClass: kSecClassGenericPassword,
    kSecAttrService: "com.prio.auth",
    kSecAttrAccount: "access_token",
    kSecValueData: tokenData,
    kSecAttrAccessible: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
]
```

```kotlin
// Android - EncryptedSharedPreferences
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

---

## Infrastructure Security

### Network Security
- VPC with private subnets for databases
- Security groups with minimal permissions
- WAF (Web Application Firewall) in front of APIs
- DDoS protection (CloudFlare/AWS Shield)
- Network segmentation between services

### Container Security
- Minimal base images (distroless where possible)
- No root users in containers
- Read-only file systems where possible
- Resource limits defined
- Regular image scanning (Trivy, Snyk)

### Kubernetes Security
```yaml
# Pod Security Policy example
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
spec:
  privileged: false
  runAsUser:
    rule: MustRunAsNonRoot
  fsGroup:
    rule: RunAsAny
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'secret'
  hostNetwork: false
  hostIPC: false
  hostPID: false
```

### Secrets Management
- HashiCorp Vault or AWS Secrets Manager
- Secrets never in code or config files
- Automatic rotation where possible
- Audit logging on all secret access
- Separate secrets per environment

---

## Secure Development

### Secure Coding Guidelines

#### General
- Never trust user input
- Use security linters (Rust: cargo-audit, Go: gosec)
- Review dependencies for vulnerabilities
- No sensitive data in logs
- Proper error handling (no stack traces to users)

#### Rust-Specific
```rust
// Use secure random
use rand::rngs::OsRng;
use rand::RngCore;

let mut key = [0u8; 32];
OsRng.fill_bytes(&mut key);

// Use constant-time comparison
use subtle::ConstantTimeEq;
if token.ct_eq(&expected_token).into() {
    // Valid
}
```

#### Go-Specific
```go
// Use crypto/rand, not math/rand
import "crypto/rand"

token := make([]byte, 32)
if _, err := rand.Read(token); err != nil {
    return err
}

// Use constant-time comparison
import "crypto/subtle"
if subtle.ConstantTimeCompare(a, b) == 1 {
    // Valid
}
```

### Dependency Management
- Dependabot or Renovate for updates
- Lock file for reproducible builds
- Regular vulnerability scanning
- No unnecessary dependencies
- Audit before adding new dependencies

### Code Review Requirements
- All changes require review
- Security-sensitive changes require security team review
- Automated security scanning in CI
- No credentials in commits (pre-commit hooks)

---

## Privacy Compliance

### GDPR Compliance

#### Legal Basis for Processing
| Processing Activity | Legal Basis |
|---------------------|-------------|
| Account management | Contract |
| Service delivery | Contract |
| Analytics | Legitimate interest |
| Marketing emails | Consent |
| AI improvement | Consent |

#### User Rights Implementation
- **Access**: Export all user data in JSON
- **Rectification**: Edit profile and preferences
- **Erasure**: Full account deletion within 30 days
- **Portability**: Data export in machine-readable format
- **Objection**: Opt-out of specific processing
- **Restriction**: Pause non-essential processing

#### Data Processing Records
Maintain records of:
- What data we collect
- Why we collect it
- Who has access
- How long we keep it
- What security measures apply

### CCPA Compliance
- "Do Not Sell My Personal Information" option
- Right to know what data is collected
- Right to delete
- Right to opt-out of sales (we don't sell data)
- Non-discrimination for exercising rights

### Privacy-Preserving Features
- On-device processing where possible
- Differential privacy for analytics
- Anonymization before analysis
- No third-party trackers
- Transparent data usage

---

## Incident Response

### Incident Classification

| Severity | Description | Response Time |
|----------|-------------|---------------|
| P1 - Critical | Data breach, full outage | 15 minutes |
| P2 - High | Partial breach, degraded service | 1 hour |
| P3 - Medium | Security vulnerability discovered | 4 hours |
| P4 - Low | Minor security issue | 24 hours |

### Response Procedure

1. **Detection & Triage**
   - Confirm incident is real
   - Classify severity
   - Assemble response team

2. **Containment**
   - Isolate affected systems
   - Preserve evidence
   - Prevent further damage

3. **Eradication**
   - Identify root cause
   - Remove threat
   - Patch vulnerabilities

4. **Recovery**
   - Restore from clean backups
   - Verify system integrity
   - Monitor for recurrence

5. **Post-Incident**
   - Document timeline
   - Conduct root cause analysis
   - Update procedures
   - Notify affected users (if required)

### Communication Templates
```
Subject: Security Update - [Date]

Dear [User],

We are writing to inform you of a security incident that may have 
affected your account...

[Details of what happened]
[What we're doing about it]
[What you should do]
[Contact information]
```

---

## Security Testing

### Testing Types

| Type | Frequency | Scope |
|------|-----------|-------|
| Static Analysis | Every commit | All code |
| Dependency Scan | Daily | All dependencies |
| Dynamic Testing | Weekly | APIs, mobile apps |
| Penetration Test | Quarterly | Full application |
| Red Team Exercise | Annually | Organization-wide |

### Security Scanning Tools
- **SAST**: Semgrep, CodeQL
- **DAST**: OWASP ZAP, Burp Suite
- **Dependencies**: Snyk, Dependabot
- **Infrastructure**: ScoutSuite, Prowler
- **Containers**: Trivy, Clair

### Vulnerability Disclosure
- security@prio.app for reports
- Response within 48 hours
- Bug bounty program (post-launch)
- Responsible disclosure timeline: 90 days

---

## Compliance & Certifications

### Target Certifications
- SOC 2 Type II (Year 1)
- ISO 27001 (Year 2)
- HIPAA (if health features added)

### Audit Trail Requirements
Log all:
- Authentication events
- Authorization decisions
- Data access (read/write)
- Configuration changes
- Admin actions
- API calls with user context

Log format:
```json
{
  "timestamp": "2025-08-15T10:30:00Z",
  "event_type": "data_access",
  "user_id": "user_123",
  "resource": "tasks",
  "action": "read",
  "ip_address": "192.168.1.1",
  "user_agent": "Prio/1.0 iOS/17.0",
  "request_id": "req_abc123"
}
```

---

## Security Checklist

### Pre-Launch
- [ ] Penetration test completed
- [ ] Security review of architecture
- [ ] All secrets in vault
- [ ] Monitoring and alerting configured
- [ ] Incident response plan documented
- [ ] Privacy policy reviewed by legal
- [ ] Cookie consent implemented
- [ ] Data retention automation tested

### Ongoing
- [ ] Weekly vulnerability scans
- [ ] Monthly access reviews
- [ ] Quarterly penetration tests
- [ ] Annual security training
- [ ] Regular backup restoration tests
- [ ] Dependency updates within SLA

---

*Document Owner: Security Expert*
*Last Updated: August 2025*
*Status: Living Document*
