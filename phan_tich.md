# Phần 1 - Phân tích logic

## 1. Tổng quan về đoạn mã InsecureTokenService

Lớp `InsecureTokenService` được sử dụng để:

- Tạo Access Token bằng JWT.
- Ký token bằng thuật toán HS256.
- Xác thực tính hợp lệ của token thông qua Secret Key.
- Thiết lập thời gian hết hạn của token.

Mặc dù JWT được sử dụng đúng về mặt kỹ thuật, nhưng cách triển khai hiện tại tồn tại hai lỗ hổng bảo mật nghiêm trọng:

1. Hardcode Secret Key trong mã nguồn.
2. Cấu hình thời gian sống của Access Token quá dài (30 ngày).

Hai lỗ hổng này làm suy giảm đáng kể khả năng bảo vệ của JWT và tạo điều kiện cho kẻ tấn công chiếm quyền truy cập hệ thống.

---

# 2. Phân tích lỗ hổng HARDCODED_SECRET_KEY

## Đoạn mã chứa lỗ hổng

```java
private static final String HARDCODED_SECRET_KEY =
    "ThisIsAVerySecretKeyButItsHardcodedAndTooShort";
```

Secret Key được khai báo trực tiếp trong source code và được sử dụng để ký cũng như xác thực JWT.

```java
private Key getSigningKey() {
    return Keys.hmacShaKeyFor(
        HARDCODED_SECRET_KEY.getBytes()
    );
}
```

---

## Tại sao đây là lỗ hổng bảo mật?

JWT sử dụng cơ chế chữ ký số (Digital Signature).

Khi tạo token:

```java
.signWith(getSigningKey(), SignatureAlgorithm.HS256)
```

Hệ thống dùng Secret Key để ký token.

Khi xác thực:

```java
Jwts.parserBuilder()
    .setSigningKey(getSigningKey())
    .build()
    .parseClaimsJws(token);
```

Hệ thống tiếp tục dùng chính Secret Key đó để kiểm tra chữ ký.

Điều này đồng nghĩa:

> Ai sở hữu Secret Key đều có thể tạo ra JWT hợp lệ.

Nếu Secret Key bị lộ thì toàn bộ cơ chế xác thực JWT gần như mất tác dụng.

---

## Các cách Secret Key có thể bị lộ

### Truy cập kho mã nguồn

Nếu repository Git bị rò rỉ hoặc bị truy cập trái phép:

```java
HARDCODED_SECRET_KEY
```

sẽ xuất hiện công khai.

---

### Reverse Engineering

Nếu ứng dụng được đóng gói thành:

```text
.jar
.war
.class
```

kẻ tấn công có thể sử dụng:

```text
JD-GUI
CFR
FernFlower
```

để dịch ngược mã nguồn và đọc được Secret Key.

---

### Lộ qua log hoặc backup

Trong nhiều trường hợp:

- File backup bị lộ.
- Log debug chứa thông tin cấu hình.
- Snapshot máy chủ bị truy cập trái phép.

Secret Key cũng có thể bị thu thập.

---

## Kịch bản tấn công

Sau khi có Secret Key:

```java
Key stolenKey =
    Keys.hmacShaKeyFor(
        HARDCODED_SECRET_KEY.getBytes()
    );
```

Kẻ tấn công có thể tự tạo JWT:

```java
String forgedToken =
    Jwts.builder()
        .setSubject("admin")
        .signWith(stolenKey, SignatureAlgorithm.HS256)
        .compact();
```

Token này sẽ được hệ thống coi là hợp lệ.

---

## Hậu quả

### Giả mạo tài khoản Admin

Kẻ tấn công có thể tạo:

```java
.setSubject("admin")
```

và chiếm toàn bộ quyền quản trị.

---

### Chiếm quyền giao dịch chứng khoán

Có thể:

- Đặt lệnh mua.
- Đặt lệnh bán.
- Hủy lệnh giao dịch.
- Truy cập danh mục đầu tư của người khác.

---

### Đánh cắp dữ liệu khách hàng

Bao gồm:

- Thông tin cá nhân.
- Số dư tài khoản.
- Lịch sử giao dịch.
- Dữ liệu tài chính nhạy cảm.

---

### Mất tính toàn vẹn hệ thống

JWT không còn đáng tin cậy vì bất kỳ ai có Secret Key đều có thể tạo token hợp lệ.

---

# 3. Phân tích lỗ hổng ACCESS_TOKEN_EXPIRATION_DAYS

## Đoạn mã chứa lỗ hổng

```java
private static final long
    ACCESS_TOKEN_EXPIRATION_DAYS = 30;
```

Token được cấp thời hạn:

```java
Instant expiry =
    now.plus(
        ACCESS_TOKEN_EXPIRATION_DAYS,
        ChronoUnit.DAYS
    );
```

Tức là:

```text
30 ngày
```

---

## Tại sao đây là lỗ hổng?

Một nguyên tắc quan trọng của JWT là:

> Access Token phải có thời gian sống ngắn.

Thông thường:

```text
5 phút
10 phút
15 phút
```

hoặc tối đa vài chục phút.

Việc để token tồn tại:

```text
30 ngày
```

làm tăng đáng kể thời gian khai thác nếu token bị đánh cắp.

---

## Kịch bản tấn công

### Bước 1

Người dùng đăng nhập hệ thống.

Hệ thống cấp token:

```text
Có hiệu lực 30 ngày
```

---

### Bước 2

Token bị đánh cắp thông qua:

- Malware.
- XSS.
- Log bị lộ.
- Trình duyệt nhiễm mã độc.
- Máy tính công cộng.

---

### Bước 3

Kẻ tấn công sử dụng token:

```http
Authorization: Bearer <stolen_token>
```

---

### Bước 4

Trong suốt 30 ngày tiếp theo:

- Token vẫn hợp lệ.
- Hệ thống vẫn chấp nhận.
- Kẻ tấn công tiếp tục truy cập.

---

## Hậu quả

### Truy cập trái phép kéo dài

Nếu token bị lộ hôm nay:

```text
Ngày 1 -> Ngày 30
```

kẻ tấn công vẫn sử dụng được.

---

### Khó phát hiện

Vì token hoàn toàn hợp lệ nên:

```java
validateToken(token)
```

vẫn trả về:

```java
true
```

Không có dấu hiệu bất thường từ góc nhìn hệ thống.

---

### Thiệt hại tài chính lớn

Trong hệ thống giao dịch chứng khoán:

- Đặt lệnh giả.
- Thao túng danh mục.
- Chuyển tài sản.
- Theo dõi dữ liệu giao dịch.

có thể diễn ra trong nhiều tuần liên tiếp.

---

# 4. Sự kết hợp nguy hiểm của hai lỗ hổng

Hai lỗ hổng này khi kết hợp tạo thành rủi ro cực kỳ nghiêm trọng.

## Bước 1

Secret Key bị lộ:

```java
HARDCODED_SECRET_KEY
```

---

## Bước 2

Kẻ tấn công tạo JWT giả mạo:

```java
.setSubject("admin")
```

---

## Bước 3

Token được cấp thời hạn:

```text
30 ngày
```

---

## Bước 4

Hệ thống xác thực thành công:

```java
service.validateToken(
    perfectlyForgedToken
);
```

Kết quả:

```java
true
```

---

## Hậu quả cuối cùng

Kẻ tấn công có thể:

- Mạo danh quản trị viên.
- Chiếm quyền người dùng bất kỳ.
- Truy cập dữ liệu tài chính nhạy cảm.
- Thực hiện giao dịch trái phép.
- Duy trì quyền truy cập trong 30 ngày.

Đây là mức độ rủi ro đặc biệt nghiêm trọng đối với một hệ thống giao dịch chứng khoán.

---

# 5. Kết luận

Đoạn mã `InsecureTokenService` tồn tại hai lỗ hổng bảo mật nghiêm trọng:

1. `HARDCODED_SECRET_KEY` làm lộ khóa ký JWT, cho phép kẻ tấn công tạo token giả mạo hoàn toàn hợp lệ.
2. `ACCESS_TOKEN_EXPIRATION_DAYS = 30` khiến token tồn tại quá lâu, mở rộng đáng kể thời gian khai thác khi token bị đánh cắp.

Khi kết hợp với nhau, hai lỗ hổng này có thể dẫn đến việc chiếm quyền tài khoản quản trị, thực hiện giao dịch trái phép, đánh cắp dữ liệu tài chính và gây thiệt hại nghiêm trọng cho toàn bộ hệ thống giao dịch chứng khoán.