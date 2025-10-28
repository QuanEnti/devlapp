#  Unit Testing Report – Authentication Module (DevCollab)

## 1. Giới thiệu

Dự án kiểm thử tự động được xây dựng cho **module Authentication** của hệ thống **DevCollab**,  
bao gồm 4 service chính:

| Service           | Chức năng |
|-------------------|------------|
| `AuthServiceImpl` | Xác thực người dùng (Local & Google OAuth2) |
| `OtpService`      | Sinh, lưu trữ và xác thực mã OTP (Redis) |
| `JwtService`      | Sinh và kiểm tra JSON Web Token |
| `MailService`     | Gửi email OTP xác thực người dùng |

Bộ test được sinh bằng **AI Prompt (GPT-5)**, sau đó được debug, tối ưu mock và chạy thực tế với **JUnit 5 + Mockito + Jacoco**.

---

## 2. Cách chạy test

### Yêu cầu môi trường

| Thành phần  | Phiên bản khuyến nghị |
|-------------|------------------------|
| Java        | 21 (hoặc ≥17) |
| Maven       | ≥ 3.9 |
| Spring Boot | 3.5.x |
| IDE         | IntelliJ IDEA / VS Code / NetBeans |
| Plugin      | Jacoco 0.8.12 (tự động qua Maven) |

### Chạy test   

```bash
# 1️ Chạy bằng terminal trong VSCode hoặc IntelliJ
./mvnw clean 

# 2️ Chạy toàn bộ test suite
./mvnw clean test
```

Sau khi chạy thành công, kiểm tra:
```
target/site/jacoco/index.html
```
→ Báo cáo coverage chi tiết theo từng class.

### Cấu trúc thư mục

```
/prompts/log.md           # Prompt & AI output qua 5 giai đoạn
/tests/                   # 28 file test JUnit 5 + Mockito
/coverage/                # Báo cáo Jacoco (HTML) xem trong coverage/jacoco/index.html
/src/main/java/...        # Source code module Authentication
/pom.xml                  # Cấu hình Maven build + plugin Jacoco
```

---

## 3. Kết quả

| Tiêu chí  | Kết quả |
|-----------|----------|
| Tổng số test | 28 |
| Pass      | 28/28 |
| Fail/Error | 0 |
| Coverage   | ~94% (Jacoco) |
| Thời gian chạy | ~9.3 giây |
| Framework      | JUnit 5 · Mockito · Jacoco |
| Strictness     | LENIENT (tránh UnnecessaryStubbingException) |

**Build result:**
```
[INFO] Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 4. Giới hạn (Limitations)

| Hạng mục                 | Mô tả giới hạn |
|--------------------------|------------------------------------------------------------|
| **Scope kiểm thử**       | Chỉ kiểm thử module Authentication (không bao gồm Controller hoặc Integration Test). |
| **Mocking Redis & Mail** | RedisTemplate và JavaMailSender được mock; không thực sự kết nối tới Redis hoặc SMTP. |
| **Token Validation**     | JWT được kiểm thử bằng secret giả (không decode ngoài môi trường thật). |
| **Coverage file**        | Jacoco chỉ đo coverage của lớp service, không bao gồm controller hoặc DTO. |
| **Thread/Async**         | Không kiểm thử concurrency hoặc async mail gửi thực tế. |
| **Performance Test**     | Không đo throughput hoặc stress test OTP generation. |

---

## 5. Rủi ro & Giải pháp

| Rủi ro tiềm ẩn               | Mô tả                                                              | Biện pháp khắc phục |
|------------------------------|--------------------------------------------------------------------|---------------------|
| **Mock sai hành vi thực tế** | Mock Redis hoặc MailSender không khớp production behavior.         | Kiểm tra lại logic integration trước deploy. |
| **False Positive Coverage**  | Một số test chỉ chạy qua hàm, chưa assert logic sâu.               | Dùng `assertAll` và verify nhiều hơn. |
| **Strict stubbing error (Mockito)** | Khi stub dư, có thể gây lỗi `UnnecessaryStubbingException`. | Giữ `@MockitoSettings(strictness = Strictness.LENIENT)` hoặc cleanup stub. |
| **Môi trường khác biệt**     | Java hoặc Spring Boot version khác có thể thay đổi API security.   | Ghim version trong `pom.xml` và test lại khi upgrade. |
| **MailService null sender**  | Nếu `senderAddress` không set, có thể lỗi SMTP.                    | Thêm test fallback & default sender. |

---

## 6. Hướng mở rộng

- **Giai đoạn 5 → 6:** gom test trùng thành `@ParameterizedTest`, thêm benchmark hiệu năng.  
- Tăng coverage lên **100%** bằng test branch nhỏ (TTL=0, token=null).  
- Tích hợp **CI/CD pipeline** với GitHub Actions hoặc Jenkins để tự động chạy test + coverage report.  
- Xuất báo cáo coverage ra PDF để nộp hoặc trình bày demo.

---

## 7. Kết luận

> **Module Authentication – Test suite đạt chuẩn Production**  
> 28/28 test PASS · Coverage 94% · Build stable & reproducible

Dự án minh họa cách **AI Prompt có thể sinh, tối ưu và chạy test JUnit thực tế**,  
đem lại **tốc độ phát triển nhanh, coverage cao**, và **code test dễ bảo trì**.

---
**Repository:** DevCollab / Authentication Module  
**Date:** 25 Oct 2025  
**License:** MIT
