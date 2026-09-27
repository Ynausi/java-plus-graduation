package ru.practicum.mapper;

import ru.practicum.dto.HitRequestDto;
import ru.practicum.model.Hit;

public class HitMapper {
    public static Hit toHit(HitRequestDto hitRequestDto) {
        return new Hit(null, hitRequestDto.getApp(), hitRequestDto.getUri(), hitRequestDto.getIp(),
                hitRequestDto.getTimestamp());
    }
}