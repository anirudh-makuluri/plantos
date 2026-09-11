package com.example.plantos.backend.machine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "machines")
public class Machine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MachineStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Machine() {
	}

	public Machine(String code, String name, MachineStatus status) {
		this.code = code;
		this.name = name;
		this.status = status;
	}

	@PrePersist
	void initializeCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public MachineStatus getStatus() {
		return status;
	}

	public void changeStatus(MachineStatus status) {
		this.status = Objects.requireNonNull(status, "status must not be null");
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
