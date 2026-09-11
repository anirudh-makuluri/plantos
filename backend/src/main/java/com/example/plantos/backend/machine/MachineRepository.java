package com.example.plantos.backend.machine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MachineRepository extends JpaRepository<Machine, Long> {

	Optional<Machine> findByCode(String code);
}
