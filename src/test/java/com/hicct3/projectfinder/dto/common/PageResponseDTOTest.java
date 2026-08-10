package com.hicct3.projectfinder.dto.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PageResponseDTOTest {

    @Test
    void keepsStablePageFieldsUsedByClients() {
        PageResponseDTO<String> result = PageResponseDTO.from(
                new PageImpl<>(List.of("첫 번째", "두 번째"), PageRequest.of(1, 2), 5));

        assertEquals(List.of("첫 번째", "두 번째"), result.getContent());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(1, result.getNumber());
        assertFalse(result.isFirst());
        assertFalse(result.isLast());
        assertFalse(result.isEmpty());
        assertEquals(2, result.getNumberOfElements());
        assertEquals(2, result.getSize());
    }
}
