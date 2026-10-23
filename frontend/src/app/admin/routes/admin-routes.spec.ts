import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import type { TestRequest } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import type { ComponentFixture } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { AdminRoutes } from './admin-routes';
import type { CreateRouteRequest } from '../../core/routes/route.model';

describe('AdminRoutes server validation', () => {
  const routeRequest: CreateRouteRequest = {
    destinationCity: 'Londra',
    destinationCountryCode: 'GB',
    returnEnabled: false
  };
  const fieldErrors = {
    destinationCity: 'Destinazione non valida.',
    destinationCountryCode: 'Paese non ammesso.',
    returnEnabled: 'Controlla il collegamento di ritorno.'
  };
  const fallbackMessage = 'Controlla i dati inseriti e riprova.';

  let fixture: ComponentFixture<AdminRoutes>;
  let component: AdminRoutes;
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminRoutes],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AdminRoutes);
    component = fixture.componentInstance;
    fixture.detectChanges();

    httpTesting.expectOne('/api/v1/routes?page=0&size=10').flush({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0
    });

    component.openForm();
    component.form.setValue(routeRequest);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function submitForm(): TestRequest {
    const element: HTMLElement = fixture.nativeElement;
    element.querySelector('form')!.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true })
    );
    fixture.detectChanges();

    return httpTesting.expectOne({
      method: 'POST',
      url: '/api/v1/routes'
    });
  }

  function rejectWithFieldErrors(): void {
    submitForm().flush({ fieldErrors }, {
      status: 400,
      statusText: 'Bad Request'
    });
    fixture.detectChanges();
  }

  it('renders each server message below its field and preserves the submitted values', () => {
    const request = submitForm();

    expect(request.request.body).toEqual(routeRequest);
    expect(component.saving()).toBe(true);
    expect(component.form.disabled).toBe(true);

    request.flush({ fieldErrors }, {
      status: 400,
      statusText: 'Bad Request'
    });
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    for (const [field, message] of Object.entries(fieldErrors)) {
      expect(
        element.querySelector('#' + field)?.nextElementSibling?.textContent
      ).toContain(message);
    }

    expect(component.form.getRawValue()).toEqual(routeRequest);
    expect(component.formVisible()).toBe(true);
    expect(component.saving()).toBe(false);
    expect(component.form.enabled).toBe(true);
    expect(component.saveErrorMessage()).toBe(fallbackMessage);
    expect(element.querySelector<HTMLInputElement>('#destinationCity')!.disabled)
      .toBe(false);
  });

  it('clears server messages when the user edits a value', () => {
    rejectWithFieldErrors();

    const element: HTMLElement = fixture.nativeElement;
    const input = element.querySelector<HTMLInputElement>('#destinationCity')!;
    input.value = 'Manchester';
    input.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();

    expect(component.serverFieldErrors()).toEqual({});
    expect(component.saveErrorMessage()).toBe('');
    for (const message of Object.values(fieldErrors)) {
      expect(element.textContent).not.toContain(message);
    }
  });

  it('clears errors and restores the initial values when cancelling and reopening', () => {
    rejectWithFieldErrors();

    component.cancelForm();
    fixture.detectChanges();
    component.openForm();
    fixture.detectChanges();

    expect(component.serverFieldErrors()).toEqual({});
    expect(component.saveErrorMessage()).toBe('');
    expect(component.form.getRawValue()).toEqual({
      destinationCity: '',
      destinationCountryCode: null,
      returnEnabled: null
    });
    expect(component.form.untouched).toBe(true);
  });

  it('clears previous field errors on a new submission and preserves duplicate handling', () => {
    rejectWithFieldErrors();

    const request = submitForm();
    expect(component.serverFieldErrors()).toEqual({});

    request.flush({}, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(component.serverFieldErrors()).toEqual({});
    expect(component.saveErrorMessage()).toBe(
      'Esiste già una tratta con questa destinazione e paese.'
    );
    expect(component.form.getRawValue()).toEqual(routeRequest);
    expect(component.form.enabled).toBe(true);
  });

  it.each([
    { name: 'a null body', body: null },
    { name: 'a text body', body: 'Bad Request' },
    { name: 'a missing map', body: {} },
    { name: 'a null map', body: { fieldErrors: null } },
    { name: 'an array instead of a map', body: { fieldErrors: ['Invalid'] } }
  ])('keeps the fallback message for $name', ({ body }) => {
    submitForm().flush(body, {
      status: 400,
      statusText: 'Bad Request'
    });
    fixture.detectChanges();

    expect(component.serverFieldErrors()).toEqual({});
    expect(component.saveErrorMessage()).toBe(fallbackMessage);
    expect(component.form.getRawValue()).toEqual(routeRequest);
    expect(component.form.enabled).toBe(true);
  });

  it('ignores unknown fields and non-text or blank messages', () => {
    submitForm().flush({
      fieldErrors: {
        destinationCity: 'Destinazione non valida.',
        destinationCountryCode: 42,
        returnEnabled: '   ',
        internalDetail: 'Messaggio interno'
      }
    }, {
      status: 400,
      statusText: 'Bad Request'
    });
    fixture.detectChanges();

    expect(component.serverFieldErrors()).toEqual({
      destinationCity: 'Destinazione non valida.'
    });
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).not.toContain('Messaggio interno');
    expect(component.saveErrorMessage()).toBe(fallbackMessage);
  });
});
