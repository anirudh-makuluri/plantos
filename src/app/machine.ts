export type MachineStatus = 'OFFLINE' | 'IDLE' | 'RUNNING' | 'FAULTED' | 'MAINTENANCE';

export interface Machine {
  id: number;
  code: string;
  name: string;
  status: MachineStatus;
  createdAt: string;
}

export interface CreateMachineRequest {
  code: string;
  name: string;
}
