import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Machine, MachineStatus } from './machine';
import { MachineService } from './machine.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  private readonly machineService = inject(MachineService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly statuses: MachineStatus[] = [
    'OFFLINE',
    'IDLE',
    'RUNNING',
    'FAULTED',
    'MAINTENANCE'
  ];
  readonly machines = signal<Machine[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly processingCode = signal<string | null>(null);
  readonly errorMessage = signal('');

  code = '';
  name = '';

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadMachines();
    }
  }

  loadMachines(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.machineService.getMachines().subscribe({
      next: (machines) => {
        this.machines.set(machines);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not reach the PlantOS backend. Make sure Spring Boot is running on port 8081.');
        this.loading.set(false);
      }
    });
  }

  registerMachine(): void {
    const code = this.code.trim().toUpperCase();
    const name = this.name.trim();

    if (!code || !name) {
      this.errorMessage.set('Enter both a machine code and name.');
      return;
    }

    this.saving.set(true);
    this.errorMessage.set('');
    this.machineService.createMachine({ code, name }).subscribe({
      next: (machine) => {
        this.machines.update((machines) => [...machines, machine]);
        this.code = '';
        this.name = '';
        this.saving.set(false);
      },
      error: () => {
        this.errorMessage.set('The machine could not be registered. Check that its code is unique.');
        this.saving.set(false);
      }
    });
  }

  changeStatus(machine: Machine, event: Event): void {
    const status = (event.target as HTMLSelectElement).value as MachineStatus;
    if (status === machine.status) {
      return;
    }

    this.processingCode.set(machine.code);
    this.errorMessage.set('');
    this.machineService.updateStatus(machine.code, status).subscribe({
      next: (updatedMachine) => {
        this.replaceMachine(updatedMachine);
        this.processingCode.set(null);
      },
      error: () => {
        this.errorMessage.set(`Could not update ${machine.code}.`);
        this.processingCode.set(null);
      }
    });
  }

  deleteMachine(machine: Machine): void {
    this.processingCode.set(machine.code);
    this.errorMessage.set('');
    this.machineService.deleteMachine(machine.code).subscribe({
      next: () => {
        this.machines.update((machines) => machines.filter(({ code }) => code !== machine.code));
        this.processingCode.set(null);
      },
      error: () => {
        this.errorMessage.set(`Could not delete ${machine.code}.`);
        this.processingCode.set(null);
      }
    });
  }

  trackByCode(_index: number, machine: Machine): string {
    return machine.code;
  }

  private replaceMachine(updatedMachine: Machine): void {
    this.machines.update((machines) =>
      machines.map((machine) => machine.code === updatedMachine.code ? updatedMachine : machine)
    );
  }
}
