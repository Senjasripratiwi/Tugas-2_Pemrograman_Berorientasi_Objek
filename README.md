# Tugas-2_Pemrograman_Berorientasi_Objek
## Deskripsi
Proyek ini adalah backend API untuk sistem pembayaran subscription sederhana yang menggunakan SQLite sebagai database. API ini memungkinkan pengguna untuk mengakses dan memanipulasi data entitas seperti customers, subscriptions, dan items.

## Instalasi
Berikut langkah-langkah untuk menginstal proyek ini secara lokal:

1. Clone repository ini:
    ```bash
    git clone https://github.com/username/simple-subscription-payment-system.git
    ```
2. Masuk ke direktori proyek:
    ```bash
    cd simple-subscription-payment-system
    ```
3. Install dependencies (jika ada):
    ```bash
    npm install
    ```
4. Jalankan server:
    ```bash
    npm start
    ```

## Penggunaan
Untuk menggunakan API ini, Anda bisa menggunakan aplikasi seperti Postman untuk melakukan pengujian. Setiap endpoint dapat diakses melalui `http://127.0.0.1:9xxx`, di mana `xxx` adalah 3 digit terakhir dari NIM Anda.

## Spesifikasi API
### Customers
- `GET /customers` - Mendapatkan daftar semua pelanggan
- `GET /customers/{id}` - Mendapatkan informasi detail pelanggan dan alamatnya
- `GET /customers/{id}/cards` - Mendapatkan daftar kartu kredit/debit milik pelanggan
- `GET /customers/{id}/subscriptions` - Mendapatkan daftar semua subscriptions milik pelanggan
- `GET /customers/{id}/subscriptions?subscriptions_status={active, cancelled, non-renewing}` - Mendapatkan daftar subscriptions berdasarkan status

### Subscriptions
- `GET /subscriptions` - Mendapatkan daftar semua subscriptions
- `GET /subscriptions?sort_by=current_term_end&sort_type=desc` - Mendapatkan daftar subscriptions diurutkan berdasarkan current_term_end secara descending
- `GET /subscriptions/{id}` - Mendapatkan informasi detail subscription, termasuk informasi pelanggan dan item

### Items
- `GET /items` - Mendapatkan daftar semua produk
- `GET /items?is_active=true` - Mendapatkan daftar produk yang memiliki status aktif
- `GET /items/{id}` - Mendapatkan informasi detail produk

### Operasi CRUD
#### POST
- `POST /customers` - Membuat pelanggan baru
- `POST /subscriptions` - Membuat subscription baru dengan customer ID, alamat pengiriman, kartu, dan item yang dibeli
- `POST /items` - Membuat item baru

#### PUT
- `PUT /customers/{id}` - Mengubah data pelanggan
- `PUT /customers/{id}/shipping_addresses/{id}` - Mengubah data alamat pengiriman pelanggan
- `PUT /items/{id}` - Mengubah data item

#### DELETE
- `DELETE /items/{id}` - Mengubah status item menjadi tidak aktif
- `DELETE /customers/{id}/cards/{id}` - Menghapus informasi kartu kredit pelanggan jika is_primary bernilai false

### Error Handling
- HTTP 404 - Jika entitas tidak ditemukan
- HTTP 400 - Jika data tidak lengkap saat membuat entitas baru
- HTTP 404 - Jika entitas tidak ditemukan saat melakukan update atau delete

## Kontribusi
Jika Anda ingin berkontribusi pada proyek ini, ikuti langkah-langkah berikut:
1. Fork repository ini.
2. Buat branch fitur baru (`git checkout -b fitur-anda`).
3. Commit perubahan Anda (`git commit -m 'Tambah fitur ABC'`).
4. Push ke branch tersebut (`git push origin fitur-anda`).
5. Buat Pull Request.

## Lisensi
Proyek ini dilisensikan di bawah lisensi MIT - lihat file [LICENSE](LICENSE) untuk detail lebih lanjut.
