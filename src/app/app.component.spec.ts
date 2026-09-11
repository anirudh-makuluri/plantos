import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AppComponent } from './app.component';
import { MachineService } from './machine.service';

describe('AppComponent', () => {
  const machineService = jasmine.createSpyObj<MachineService>('MachineService', [
    'getMachines',
    'createMachine',
    'updateStatus',
    'deleteMachine'
  ]);

  beforeEach(async () => {
    machineService.getMachines.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [{ provide: MachineService, useValue: machineService }]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should load and render the machine registry', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(machineService.getMachines).toHaveBeenCalled();
    expect(compiled.querySelector('h1')?.textContent).toContain('Machine registry');
  });
});
