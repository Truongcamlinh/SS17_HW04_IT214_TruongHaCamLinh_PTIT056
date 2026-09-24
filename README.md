# SS17_HW04 - Chống Cache Stampede với thuộc tính sync

**Sinh viên:** Trương Hà Cẩm Linh - **Mã sinh viên:** PTIT056

## 1. Vấn đề Cache Stampede

Khi key của một sản phẩm bán chạy hết hạn, nhiều request có thể cùng nhận cache miss. Nếu không đồng bộ, mỗi request đều chạy truy vấn nặng xuống database. Với 50 request và truy vấn mất 2 giây, database phải xử lý 50 truy vấn gần như cùng lúc, dễ làm cạn connection pool và tăng CPU.

Giải pháp của bài là dùng:

```java
@Cacheable(value = "flash-sale", key = "#id", sync = true)
public Product getProductById(Long id) {
    log.info("Fetching from Database for product {}", id);
    Thread.sleep(2000);
    return productRepository.findById(id).orElseThrow();
}
```

Khi nhiều luồng cùng yêu cầu một key chưa có, cache chỉ cho một luồng thực thi method để tạo dữ liệu. Các luồng khác chờ cùng kết quả. Sau khi dữ liệu được nạp, toàn bộ request nhận cùng sản phẩm.

## 2. Input và output

- Input: `id` kiểu `Long`, ví dụ `1`.
- Output: `Product` gồm `id`, `name`, `price`.
- API minh họa: `GET /api/products/{id}`.
- Cache: `flash-sale`, key là `#id`, TTL 5 phút.

## 3. Vì sao dùng Caffeine?

Bài sử dụng Caffeine vì Spring Cache hỗ trợ lời gọi tải nguyên tử theo key cho `sync = true`. Đây là khóa trong phạm vi cache của một tiến trình ứng dụng. Nó phù hợp để chứng minh rõ cơ chế bằng 50 luồng trong một instance.

`sync = true` không phải distributed lock giữa nhiều instance. Nếu triển khai nhiều bản sao service với Redis, mỗi instance vẫn có thể cùng truy vấn DB khi key hết hạn nếu cache provider không cung cấp distributed single-flight. Khi đó cần bổ sung Redis lock/Redisson, request coalescing phân tán hoặc chủ động cache warm-up.

## 4. Kiểm thử 50 request đồng thời

Test `FlashSaleProductServiceTest` thực hiện:

1. Xóa cache `flash-sale` và đưa bộ đếm DB về 0.
2. Tạo thread pool 50 luồng.
3. Dùng 50 `CompletableFuture` gọi cùng `getProductById(1L)`.
4. Chờ tất cả hoàn thành và kiểm tra đủ 50 kết quả thành công.
5. Xác nhận bộ đếm truy xuất DB bằng 1.
6. Xác nhận tổng thời gian nằm trong khoảng 1,9 đến 4 giây, thay vì 100 giây.

Chạy kiểm thử:

```bash
./gradlew clean test
```

Trong log chỉ có đúng một dòng:

```text
Fetching from Database for product 1
```

## 5. So sánh sync false và sync true

| Tiêu chí | Không có `sync = true` | Có `sync = true` |
|---|---|---|
| Cache miss đồng thời | Nhiều luồng cùng chạy method | Một luồng tạo dữ liệu cho mỗi key |
| Số truy vấn DB trong thử nghiệm | Có thể gần 50 | Chính xác 1 |
| Tải tức thời lên DB | Cao | Thấp |
| Thời gian mỗi request | Khoảng 2 giây nhưng DB chịu tải lớn | Khoảng 2 giây và các request dùng chung kết quả |
| Phạm vi bảo vệ | Không có single-flight | Trong một instance/cache provider hỗ trợ |

Các request không chạy tuần tự 50 lần. Một request mất khoảng 2 giây để tạo cache; 49 request còn lại chờ kết quả đó nên tổng thời gian vẫn xấp xỉ 2 giây cộng overhead.

## 6. Chạy ứng dụng

```bash
./gradlew bootRun
curl http://localhost:8080/api/products/1
```

Dữ liệu H2 ban đầu là sản phẩm id `1`, tên `Điện thoại Flash Sale`, giá `9990000`. Cache có `maximumSize=1000` và `expireAfterWrite=5m`.

## 7. Kết luận

`@Cacheable(sync = true)` ngăn Cache Stampede theo cùng key trong một instance bằng cách hợp nhất các lần tải đồng thời. Test tự động là bằng chứng lặp lại được: 50 lời gọi thành công, một lần truy vấn DB và thời gian tổng gần bằng một truy vấn 2 giây.
