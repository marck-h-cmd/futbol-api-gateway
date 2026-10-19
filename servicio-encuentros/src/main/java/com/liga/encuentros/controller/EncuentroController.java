package com.liga.encuentros.controller;

import com.liga.encuentros.dto.EncuentroRequestDTO;
import com.liga.encuentros.dto.EncuentroResponseDTO;
import com.liga.encuentros.dto.PosicionDTO;
import com.liga.encuentros.service.EncuentroService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/encuentros")
@CrossOrigin(origins = "*")
public class EncuentroController {

    private final EncuentroService encuentroService;

    public EncuentroController(EncuentroService encuentroService) {
        this.encuentroService = encuentroService;
    }

    @PostMapping
    public ResponseEntity<?> registrarEncuentro(@RequestBody EncuentroRequestDTO dto) {
        try {
            EncuentroResponseDTO creado = encuentroService.registrarEncuentro(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<EncuentroResponseDTO>> listarEncuentros() {
        return ResponseEntity.ok(encuentroService.listarEncuentros());
    }

    @GetMapping("/tabla-posiciones")
    public ResponseEntity<?> obtenerTablaPosiciones() {
        try {
            List<PosicionDTO> tabla = encuentroService.obtenerTablaPosiciones();
            return ResponseEntity.ok(tabla);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        }
    }
}
