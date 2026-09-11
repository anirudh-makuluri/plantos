import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateMachineRequest, Machine, MachineStatus } from './machine';

@Injectable({ providedIn: 'root' })
export class MachineService {
  private readonly http = inject(HttpClient);
  private readonly machinesUrl = '/api/machines';

  getMachines(): Observable<Machine[]> {
    return this.http.get<Machine[]>(this.machinesUrl);
  }

  createMachine(request: CreateMachineRequest): Observable<Machine> {
    return this.http.post<Machine>(this.machinesUrl, request);
  }

  updateStatus(code: string, status: MachineStatus): Observable<Machine> {
    return this.http.patch<Machine>(
      `${this.machinesUrl}/${encodeURIComponent(code)}/status`,
      { status }
    );
  }

  deleteMachine(code: string): Observable<void> {
    return this.http.delete<void>(`${this.machinesUrl}/${encodeURIComponent(code)}`);
  }
}
