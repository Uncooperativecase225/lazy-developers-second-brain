import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import App from './App';

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

describe('App', () => {
  it('shows the onboarding screen when no projects are registered', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith('/api/system/status')) return mockResponse({ application: 'ok', database: 'connected', ollama: 'connected', model: 'qwen3.5:9b', mode: 'local-first' });
      if (url.endsWith('/api/projects')) return mockResponse([]);
      return mockResponse({});
    });
    vi.stubGlobal('fetch', fetchMock);
    render(<App />);
    expect(await screen.findByText(/Less remembering/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Add your first repository/i }));
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(screen.getByLabelText(/Project name/i)).toBeInTheDocument();
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
  });
});

function mockResponse(body: unknown): Response {
  return { ok: true, status: 200, json: async () => body } as Response;
}
