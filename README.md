# FixIt - Citizen Report Application 🛠️

**FixIt** adalah aplikasi pelaporan fasilitas publik berbasis mobile yang dirancang khusus untuk memudahkan warga dalam mengadukan kerusakan fasilitas umum (seperti jalan berlubang, lampu jalan mati, atau saluran air tersumbat) secara instan. 

Proyek ini dibangun sebagai bagian dari tugas laboratorium pemrograman mobile menggunakan framework **Nylo 7 (Flutter)** dan **Supabase** sebagai *backend-as-a-service* (BaaS).

---

## Fitur Utama

- **Authentication Guard**: Mengamankan aplikasi sehingga hanya pengguna terautentikasi yang dapat masuk ke dalam sistem.
- **Sleek & Simple UI**: Antarmuka bersih dan responsif yang berfokus pada fungsionalitas dan kemudahan penggunaan.
- **Real-time Explore Page**: Menampilkan seluruh daftar keluhan warga secara dinamis yang ditarik langsung dari database cloud.
- **Report Submission**: Form pengaduan instan untuk mengirimkan judul keluhan beserta deskripsi detail kerusakan ke sistem.

---

## Tech Stack & Arsitektur

- **Frontend SDK**: Flutter & Dart
- **Framework**: Nylo 7 (MVC-like pattern dengan pemisahan Page & Controller)
- **Backend & Database**: Supabase (PostgreSQL)
- **Environment Management**: Nylo Env (Generated via Build Runner)

---

## Prasyarat Instalasi

Sebelum menjalankan proyek ini secara lokal, pastikan kamu telah menginstal:
- Flutter SDK (Versi terbaru direkomendasikan)
- Nylo v7
- Akun Supabase (untuk mendapatkan API URL dan Anon/Publishable Key)

---

## Langkah Konfigurasi & Instalasi

### Kloning Repositori
```bash
git clone [https://github.com/username-kamu/fixit.git](https://github.com/username-kamu/fixit.git)
cd fixit
