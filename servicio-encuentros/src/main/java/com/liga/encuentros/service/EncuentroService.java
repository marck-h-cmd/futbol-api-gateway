package com.liga.encuentros.service;

import com.liga.encuentros.client.EquipoClient;
import com.liga.encuentros.dto.EncuentroRequestDTO;
import com.liga.encuentros.dto.EncuentroResponseDTO;
import com.liga.encuentros.dto.EquipoDTO;
import com.liga.encuentros.dto.PosicionDTO;
import com.liga.encuentros.entity.Encuentro;
import com.liga.encuentros.repository.EncuentroRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EncuentroService {

    private final EncuentroRepository encuentroRepository;
    private final EquipoClient equipoClient;

    public EncuentroService(EncuentroRepository encuentroRepository, EquipoClient equipoClient) {
        this.encuentroRepository = encuentroRepository;
        this.equipoClient = equipoClient;
    }

    @Transactional
    public EncuentroResponseDTO registrarEncuentro(EncuentroRequestDTO dto) {
        if (dto.getEquipoLocalId() == null || dto.getEquipoVisitanteId() == null) {
            throw new IllegalArgumentException("Los IDs de los equipos local y visitante son obligatorios.");
        }

        if (dto.getEquipoLocalId().equals(dto.getEquipoVisitanteId())) {
            throw new IllegalArgumentException("Un equipo no puede enfrentarse consigo mismo.");
        }

        if (dto.getGolesLocal() == null || dto.getGolesLocal() < 0 ||
            dto.getGolesVisitante() == null || dto.getGolesVisitante() < 0) {
            throw new IllegalArgumentException("Los goles no pueden ser nulos ni negativos.");
        }

        if (dto.getFecha() == null) {
            throw new IllegalArgumentException("La fecha del encuentro es obligatoria.");
        }

        EquipoDTO local = buscarEquipo(dto.getEquipoLocalId(), "local");
        EquipoDTO visitante = buscarEquipo(dto.getEquipoVisitanteId(), "visitante");

        Encuentro encuentro = new Encuentro(
                dto.getEquipoLocalId(),
                dto.getEquipoVisitanteId(),
                dto.getGolesLocal(),
                dto.getGolesVisitante(),
                dto.getFecha()
        );

        Encuentro guardado = encuentroRepository.save(encuentro);

        return new EncuentroResponseDTO(
                guardado.getId(),
                local.getId(),
                local.getNombre(),
                visitante.getId(),
                visitante.getNombre(),
                guardado.getGolesLocal(),
                guardado.getGolesVisitante(),
                guardado.getFecha()
        );
    }

    @Transactional(readOnly = true)
    public List<EncuentroResponseDTO> listarEncuentros() {
        Map<Long, String> equiposMap = new HashMap<>();
        try {
            List<EquipoDTO> equipos = equipoClient.listarEquipos();
            if (equipos != null) {
                for (EquipoDTO eq : equipos) {
                    equiposMap.put(eq.getId(), eq.getNombre());
                }
            }
        } catch (Exception e) {
            // Continua con identificador por defecto si el cliente presenta demora o excepcion
        }

        return encuentroRepository.findAll().stream()
                .map(enc -> new EncuentroResponseDTO(
                        enc.getId(),
                        enc.getEquipoLocalId(),
                        equiposMap.getOrDefault(enc.getEquipoLocalId(), "Equipo #" + enc.getEquipoLocalId()),
                        enc.getEquipoVisitanteId(),
                        equiposMap.getOrDefault(enc.getEquipoVisitanteId(), "Equipo #" + enc.getEquipoVisitanteId()),
                        enc.getGolesLocal(),
                        enc.getGolesVisitante(),
                        enc.getFecha()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PosicionDTO> obtenerTablaPosiciones() {
        Map<Long, PosicionDTO> tablaMap = new HashMap<>();
        try {
            List<EquipoDTO> equipos = equipoClient.listarEquipos();
            if (equipos != null) {
                for (EquipoDTO eq : equipos) {
                    tablaMap.put(eq.getId(), new PosicionDTO(eq.getId(), eq.getNombre()));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error al obtener la lista de equipos para la tabla de posiciones: " + e.getMessage());
        }

        List<Encuentro> encuentros = encuentroRepository.findAll();

        for (Encuentro enc : encuentros) {
            PosicionDTO localDTO = tablaMap.get(enc.getEquipoLocalId());
            PosicionDTO visitaDTO = tablaMap.get(enc.getEquipoVisitanteId());

            if (localDTO != null && visitaDTO != null) {
                localDTO.setPartidosJugados(localDTO.getPartidosJugados() + 1);
                visitaDTO.setPartidosJugados(visitaDTO.getPartidosJugados() + 1);

                localDTO.setGolesAFavor(localDTO.getGolesAFavor() + enc.getGolesLocal());
                localDTO.setGolesEnContra(localDTO.getGolesEnContra() + enc.getGolesVisitante());

                visitaDTO.setGolesAFavor(visitaDTO.getGolesAFavor() + enc.getGolesVisitante());
                visitaDTO.setGolesEnContra(visitaDTO.getGolesEnContra() + enc.getGolesLocal());

                if (enc.getGolesLocal() > enc.getGolesVisitante()) {
                    localDTO.setPartidosGanados(localDTO.getPartidosGanados() + 1);
                    visitaDTO.setPartidosPerdidos(visitaDTO.getPartidosPerdidos() + 1);
                } else if (enc.getGolesVisitante() > enc.getGolesLocal()) {
                    visitaDTO.setPartidosGanados(visitaDTO.getPartidosGanados() + 1);
                    localDTO.setPartidosPerdidos(localDTO.getPartidosPerdidos() + 1);
                } else {
                    localDTO.setPartidosEmpatados(localDTO.getPartidosEmpatados() + 1);
                    visitaDTO.setPartidosEmpatados(visitaDTO.getPartidosEmpatados() + 1);
                }
            }
        }

        List<PosicionDTO> tablaOrdenada = new ArrayList<>(tablaMap.values());
        Collections.sort(tablaOrdenada);
        return tablaOrdenada;
    }

    private EquipoDTO buscarEquipo(Long id, String rol) {
        try {
            EquipoDTO equipo = equipoClient.obtenerPorId(id);
            if (equipo == null) {
                throw new IllegalArgumentException("No existe el equipo " + rol + " con ID: " + id);
            }
            return equipo;
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("No existe el equipo " + rol + " con ID: " + id);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al consultar el equipo " + rol + " con ID: " + id + ": " + e.getMessage());
        }
    }
}
