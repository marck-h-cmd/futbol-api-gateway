package com.liga.encuentros.repository;

import com.liga.encuentros.entity.Encuentro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EncuentroRepository extends JpaRepository<Encuentro, Long> {

    List<Encuentro> findByEquipoLocalIdOrEquipoVisitanteId(Long equipoLocalId, Long equipoVisitanteId);
}
