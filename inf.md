
---

## 🌐 **Resumo dos Endpoints**

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/bookings`               | Criar nova reserva |
| `GET` | `/api/bookings/{token}`        | Consultar reserva |
| `PUT` | `/api/bookings/{token}/cancel` | Cancelar reserva |
| `GET` | `/api/bookings/municipalities` | Listar municípios |
| `GET` | `/api/staff/bookings`          | Listar todas reservas (staff) |
| `PATCH` | `/api/staff/bookings/{token}/status` | Atualizar estado (staff) |



---

## ✅ **Requisitos Atendidos pelos Endpoints**

### **1. "Cidadão pode solicitar coleta para data/horário específico"**
```java
@PostMapping("/api/bookings")
public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request)
```
- **Date/Time**: `BookingRequest` tem `preferredDate` (LocalDateTime)
- **Descrição dos itens**: `BookingRequest` tem `itemsDescription`

### **2. "Escolher município de lista fechada"**
```java
@GetMapping("/api/bookings/municipalities")
public ResponseEntity<List<String>> getAvailableMunicipalities()
```
- **Lista fechada**: Este endpoint retorna a lista válida de municípios
- **Obrigatório**: No `BookingRequest`, `municipality` tem `@NotBlank`

### **3. "Token de acesso para consultas/cancelamento"**
```java
@GetMapping("/api/bookings/{token}")
public ResponseEntity<BookingResponse> getBookingByToken(@PathVariable String token)

@PutMapping("/api/bookings/{token}/cancel")
public ResponseEntity<BookingResponse> cancelBooking(@PathVariable String token)
```
- **Token único**: Gerado automaticamente como UUID
- **Cancelar**: Endpoint PUT específico para cancelamento

### **4. "Estados com timestamp na evolução"**
```java
// No modelo Booking:
private List<StatusHistoryEntry> statusHistory = new ArrayList<>();

public void addStatusHistory(BookingStatus status, String notes) {
    this.statusHistory.add(new StatusHistoryEntry(status, LocalDateTime.now(), notes));
}
```


### **5. "Staff pode ver e atualizar estados"**
```java
@GetMapping("/api/staff/bookings")
public ResponseEntity<List<BookingResponse>> getAllBookings()

@PatchMapping("/api/staff/bookings/{token}/status")
public ResponseEntity<BookingResponse> updateBookingStatus()
```

- **Ver todas**: Endpoint staff vê todos os services requests
- **Atualiza estados** :Endpoint específico para mudança de status

---
