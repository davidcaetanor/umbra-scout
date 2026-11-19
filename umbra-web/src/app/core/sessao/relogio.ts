import { InjectionToken } from '@angular/core';

export const RELOGIO = new InjectionToken<() => number>('RELOGIO', { providedIn: 'root', factory: () => Date.now });
