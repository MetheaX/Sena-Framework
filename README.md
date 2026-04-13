# Sena Framework

Sena is a plug-and-play authentication and access-management framework for **Spring Boot RESTful services** under **MetheaX Project**.
It handles user authentication, role-based permission checks, and JWT/JWE token lifecycle so you don't have to.

A companion [UI Management Tool](https://github.com/MetheaX/methea-management) is available for managing users, roles, and permissions through a browser interface *(under development)*.

---

## Features

- **JWT / JWE tokens** — encrypted access tokens and refresh tokens via [Nimbus JOSE+JWT](https://connect2id.com/products/nimbus-jose-jwt)
- **Argon2 password hashing** — modern, memory-hard password encoding
- **RSA key-pair encryption** — 2048-bit keys loaded from PKCS12 keystores
- **Role & resource-based authorization** — fine-grained URI permission control
- **Public URI whitelist** — selectively expose endpoints without authentication
- **Session management** — server-side session tracking with token revocation support
- **Spring Data JPA auditing** — automatic `createdBy` / `updatedBy` population

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Security | Spring Security 6 |
| Token format | JWE (RSA-OAEP-256 + A256GCM) |
| Password hashing | Argon2id |
| Cryptography | BouncyCastle |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL (tested) |
| Build | Maven |

---

## Module Structure

```
methea/
├── sena-core/     # Domain entities, repositories, utilities, security primitives
├── sena-auth/     # Authentication REST API (token issue, refresh, revoke)
└── sena-auth-app/ # Sample host application
```

### Domain Model

| Entity | Description |
|---|---|
| `Account` | Organisation / company |
| `Group` | Department or team within an account |
| `User` | System user with Argon2-encoded password |
| `Role` | Named role assigned to users |
| `Resource` | API endpoint URI |
| `Permission` | Binds a `Role` to a `Resource` |
| `PublicPermission` | Whitelisted (unauthenticated) URIs and their allowed HTTP methods |
| `SessionManagement` | Active token sessions — used for revocation |
| `JWTConfig` | JWT expiry and signing configuration |

---

## REST API

All endpoints are under `/auth/**` and are publicly accessible.

| Method | Path | Description |
|---|---|---|
| `POST` | `/auth/token` | Obtain access + refresh tokens |
| `POST` | `/auth/refresh/token` | Exchange a refresh token for a new access token |
| `POST` | `/auth/token/revoke` | Revoke an active access token |

### Token request
```json
POST /auth/token
{
  "username": "admin",
  "password": "secret"
}
```

### Token response
```json
{
  "status": 200,
  "message": "Access token generated!!!",
  "token": {
    "accessToken": "<JWE>",
    "refreshToken": "<JWE>",
    "tokenType": "Bearer ",
    "expiredIn": "1234567890"
  }
}
```

Pass the access token in subsequent requests:
```
Authorization: Bearer <JWE>
```

---

## Getting Started

### Prerequisites

- JDK 21+
- Maven 3.8+
- PostgreSQL
- [KeyStore Explorer](https://keystore-explorer.org/) or `keytool` to generate PKCS12 keystores

### Build

```bash
git clone https://github.com/MetheaX/Sena-Framework.git
cd Sena-Framework
mvn clean install
```

### Integrate into your application

**1. Add the dependency**

```xml
<dependency>
    <groupId>com.metheax.sena</groupId>
    <artifactId>sena-auth</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

**2. Configure component scan**

```java
@SpringBootApplication(scanBasePackages = {"com.metheax.sena", "com.yourcompany"})
@EnableJpaRepositories(basePackages = {"com.metheax.sena", "com.yourcompany"})
@EntityScan(basePackages = {"com.metheax.sena", "com.yourcompany"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

**3. Configure your datasource** (`application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/yourdb
    username: youruser
    password: yourpassword
  jpa:
    hibernate:
      ddl-auto: none
```

**4. Configure keystores**

Generate two PKCS12 keystores — one for access tokens, one for refresh tokens — then add:

```yaml
keystore-token-file: classpath:keystore/token.pfx
keystore-token-password: changeit
keystore-token-alias: token
keystore-key-token-password: changeit

keystore-refresh-token-file: classpath:keystore/refresh.pfx
keystore-refresh-token-password: changeit
keystore-refresh-token-alias: refresh
keystore-key-refresh-token-password: changeit
```

Place the `.pfx` files under `src/main/resources/keystore/`.

**5. Seed initial data**

```sql
create table public.core_account
(
    account_id        uuid         not null
        primary key,
    created_date_time timestamp(6) not null,
    created_user      varchar(255) not null,
    status            varchar(255) not null,
    updated_date_time timestamp(6) not null,
    updated_user      varchar(255) not null,
    account_address   varchar(255),
    account_code      varchar(18)  not null
        constraint uk_hsj408ivvnh2bdt44kfuk7idj
            unique,
    account_email     varchar(255) not null,
    account_name      varchar(255) not null,
    account_name_oth  varchar(255)
);

create table public.core_group
(
    group_id          uuid         not null
        primary key,
    created_date_time timestamp(6) not null,
    created_user      varchar(255) not null,
    status            varchar(255) not null,
    updated_date_time timestamp(6) not null,
    updated_user      varchar(255) not null,
    group_code        varchar(18)  not null
        constraint uk_2lnk0anatcyr6joii8q8875gf
            unique,
    group_name        varchar(255) not null,
    group_name_oth    varchar(255),
    remarks           varchar(255),
    account_id        uuid
        constraint fk17a6n4m0txbje20tvkvrxyndy
            references public.core_account
);

create table public.core_resource
(
    resource_id       uuid         not null
        primary key,
    created_date_time timestamp(6) not null,
    created_user      varchar(255) not null,
    status            varchar(255) not null,
    updated_date_time timestamp(6) not null,
    updated_user      varchar(255) not null,
    resource_name     varchar(255) not null
);

create table public.core_role
(
    role_id           uuid         not null
        primary key,
    created_date_time timestamp(6) not null,
    created_user      varchar(255) not null,
    status            varchar(255) not null,
    updated_date_time timestamp(6) not null,
    updated_user      varchar(255) not null,
    role_code         varchar(18)  not null
        constraint uk_1gg2nrc02x844mvv3emagd028
            unique,
    name              varchar(255) not null,
    name_oth          varchar(255)
);

create table public.core_user
(
    user_id           uuid         not null
        primary key,
    created_date_time timestamp(6) not null,
    created_user      varchar(255) not null,
    status            varchar(255) not null,
    updated_date_time timestamp(6) not null,
    updated_user      varchar(255) not null,
    email             varchar(50)  not null,
    first_name        varchar(255) not null,
    first_name_oth    varchar(255),
    frc_usr_rst_pwd   varchar(255) not null,
    identity_code     varchar(36)  not null
        constraint uk_g39bhm0fghjanmimjtanmm61d
            unique,
    last_name         varchar(255) not null,
    last_name_oth     varchar(255),
    password          varchar(512) not null,
    phone             varchar(36)  not null,
    username          varchar(36)  not null
        constraint uk_3uh3xmyn5o3y6f0l4wpw70sxi
            unique,
    group_id          uuid
        constraint fk6dgo6ua3wt540x79xcsu97t00
            references public.core_group
);

create table public.core_user_roles
(
    user_id uuid not null
        constraint fkolro2g14yumlt360tgkhs7vlf
            references public.core_user,
    role_id uuid not null
        constraint fkegqqo1brqtlln5upw6nosxiwm
            references public.core_role
);

create table public.core_permission
(
    permission_id     uuid         not null
        primary key,
    created_date_time timestamp(6) not null,
    created_user      varchar(255) not null,
    status            varchar(255) not null,
    updated_date_time timestamp(6) not null,
    updated_user      varchar(255) not null,
    resource_id       uuid
        constraint fk67ne4ds4c5u0dy8yv4osk696x
            references public.core_resource,
    role_id           uuid
        constraint fk5k9qirg101hltbfqdmjhjj6a4
            references public.core_role
);

create table public.core_jwt_config
(
    jwt_id                uuid         not null
        primary key,
    created_date_time     timestamp(6) not null,
    created_user          varchar(255) not null,
    status                varchar(255) not null,
    updated_date_time     timestamp(6) not null,
    updated_user          varchar(255) not null,
    access_token_timeout  bigint       not null,
    header                varchar(255) not null,
    refresh_token_timeout bigint       not null,
    token_type            varchar(255) not null
);

create table public.core_perm_allowed_method
(
    public_perm_id uuid not null
        constraint fkg23h11l7oqn46i2a1jkulsk4s
            references public.core_public_permission,
    http_method    varchar(255)
);

create table public.core_public_permission
(
    public_perm_id    uuid         not null
        primary key,
    created_date_time timestamp(6) not null,
    created_user      varchar(255) not null,
    status            varchar(255) not null,
    updated_date_time timestamp(6) not null,
    updated_user      varchar(255) not null,
    resource_id       uuid
        constraint uk_f7nd43um0dwxa3ggw9455liyy
            unique
        constraint fk917dkejugopuforvuq1cirbi3
            references public.core_resource
);

create table public.core_session_management
(
    id                uuid         not null
        primary key,
    created_date_time timestamp(6) not null,
    created_user      varchar(255) not null,
    status            varchar(255) not null,
    updated_date_time timestamp(6) not null,
    updated_user      varchar(255) not null,
    is_logout         boolean      not null,
    session_id        varchar(255) not null,
    user_login_id     varchar(255) not null
);

```

```sql
INSERT INTO core_account VALUES ('68bcf443-1b0c-49ff-877e-8650477383e8', NOW(), 'System', 'A', NOW(), 'System',
    'Phnom Penh, Cambodia.', 'METHEA', 'methea@localhost.com', 'Methea LLC.', 'មេធា');

INSERT INTO core_group VALUES ('366a7028-b623-49b0-8988-d711647051a5', NOW(), 'System', 'A', NOW(), 'System',
    'M_SYS_ADMIN', 'System Admin', 'អេតមីនប្រព័ន្ធ', null, '68bcf443-1b0c-49ff-877e-8650477383e8');

INSERT INTO core_resource VALUES ('ad5ea55c-547a-4537-8797-e3714c64d8a3', NOW(), 'System', 'A', NOW(), 'System', '/**');

INSERT INTO core_role VALUES ('33525a14-0ebc-4a3e-ada5-bd3ef94c9495', NOW(), 'System', 'A', NOW(), 'System',
    'ROLE_ADMIN', 'Admin', 'អេតមីន');

INSERT INTO core_user VALUES ('c6eff227-d496-486c-a72c-db2f92e06faa', NOW(), 'System', 'A', NOW(), 'System',
    'admin@localhost.com', 'Admin', 'អេតមីន', 'N', '639691', 'Admin', 'អេតមីន',
    'Gp/lBVv1VTKk3DLGiENtX4Ow1xLEUqIDRBpa+zuTJWT3IIqfC4m0SB1tpVDo1+BQHFnY90XmvbJE1JAMDfRSmA$vkDevdjMM9v+/BonOi6HX/+v3Syh5mrSMXssz5707LSlOrCiIj3O7Q50bg7mLeCl6GYKhad7GGQi7CKvP1KqnbvtS2eZMFevVHsYHsQD2UeMQqiP7nAQ1z12ZHMNc5QckXZgUPMqetTzhJMzDduJ9+nOrC3HlqSo43VoMg0k+EA',
    '+85569639691', 'admin', '366a7028-b623-49b0-8988-d711647051a5');

INSERT INTO core_user_roles VALUES ('c6eff227-d496-486c-a72c-db2f92e06faa', '33525a14-0ebc-4a3e-ada5-bd3ef94c9495');

INSERT INTO core_permission VALUES ('bdee9930-cbde-4e62-96d8-91df821a698b', NOW(), 'System', 'A', NOW(), 'System',
    'ad5ea55c-547a-4537-8797-e3714c64d8a3', '33525a14-0ebc-4a3e-ada5-bd3ef94c9495');
```

Default credentials: `admin` / `admin` *(change immediately in production)*.

---

## Running Tests

```bash
mvn test -pl sena-core,sena-auth
```

The test suite covers **85%+ instruction coverage** (enforced by JaCoCo) across both modules:

| Module | Tests | Coverage |
|---|---|---|
| `sena-core` | 140 | ≥ 85% |
| `sena-auth` | 35 | ≥ 85% |

---

## Sample Project

See [Methea-Sample-Webservice](https://github.com/MetheaX/Methea-Sample-Webservice) for a complete integration example.

---

## Support

- **Bug reports / feature requests:** open a [GitHub Issue](https://github.com/MetheaX/Methea-Framework/issues)
- **Commercial support / customisation:** kuylim.tith@outlook.com
