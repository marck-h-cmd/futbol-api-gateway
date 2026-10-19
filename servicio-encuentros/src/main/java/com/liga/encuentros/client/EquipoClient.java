package com.liga.encuentros.client;

import com.liga.encuentros.dto.EquipoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "servicio-equipos")
public interface EquipoClient {

    @GetMapping("/api/equipos/{id}")
    EquipoDTO obtenerPorId(@PathVariable("id") Long id);

    @GetMapping("/api/equipos")
    List<EquipoDTO> listarEquipos();
}
