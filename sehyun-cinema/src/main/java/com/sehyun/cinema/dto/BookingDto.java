package com.sehyun.cinema.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class BookingDto {
    private Long movieId;
    private String theaterType;
    private String seatInfo;
    private Integer adultCount;
    private Integer teenCount;
    private String paymentMethod;
    private Integer discountAmount;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime showtime;
}
