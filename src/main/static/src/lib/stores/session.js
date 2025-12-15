import { writable, get, derived } from 'svelte/store';
import { browser } from '$app/environment';
import { LiveTelemetryServiceDefinition } from '$lib/grpc/live_telemetry_service';
import { createChannel, createClient } from 'nice-grpc-web';

const initialState = {
    connected: false,
    sessionKey: null,
    drivers: new Map(),
    laps: [],
    stints: [],
    otherCarLaps: [],
    otherCarStints: [],
    telemetry: {},
    activeQueries: []
};

function createSessionStore() {
    const { subscribe, set, update } = writable(initialState);

    let client = null;

    // We use AbortControllers to cancel streams instead of .unsubscribe()
    let mainSessionAbort = null;
    let telemetryAbort = null;

    return {
        subscribe,

        connect: (sessionId, subSessionId, simSessionNumber, carNumber) => {
            // 1. SSR Guard: Don't run this on the server
            if (!browser) return;

            if (mainSessionAbort) mainSessionAbort.abort();
            mainSessionAbort = new AbortController();
            const signal = mainSessionAbort.signal;

            const channel = createChannel(window.location.origin + '/api');
            client = createClient(LiveTelemetryServiceDefinition, channel);

            const sessionIdent = {
                sessionId: Number(sessionId),
                subSessionId: Number(subSessionId),
                simSessionNumber: Number(simSessionNumber),
                carNumber: carNumber
            };

            const request = { sessionIdentifier: sessionIdent };

            // --- STREAM 1: Laps (Async Loop) ---
            // We define an async function and call it immediately without awaiting it
            // so it runs in the background.
            (async () => {
                try {
                    for await (const response of client.monitorLaps(request, { signal })) {
                        update(state => {
                            const newState = { ...state };
                            if (response.driverLap) newState.laps = [response.driverLap, ...state.laps];
                            if (response.driverStint) newState.stints = [response.driverStint, ...state.stints];
                            if (response.otherCarLap) newState.otherCarLaps = [response.otherCarLap, ...state.otherCarLaps];
                            if (response.otherCarStint) newState.otherCarStints = [response.otherCarStint, ...state.otherCarStints];
                            return newState;
                        });
                    }
                } catch (err) {
                    if (err.name !== 'AbortError') console.error("Lap stream error:", err);
                }
            })();

            // --- STREAM 2: Drivers (Async Loop) ---
            // Assumed method name 'monitorSessionInfo' based on camelCase convention
            (async () => {
                try {
                    for await (const response of client.monitorSessionInfo(request, { signal })) {
                        const driverMap = new Map();
                        if (response.drivers) {
                            response.drivers.forEach(d => {
                                driverMap.set(d.carId, d);
                            });
                            update(state => ({ ...state, drivers: driverMap }));
                        }
                    }
                } catch (err) {
                    if (err.name !== 'AbortError') console.error("Driver stream error:", err);
                }
            })();

            // Mark as connected
            update(state => ({
                ...state,
                connected: true,
                sessionKey: sessionIdent
            }));
        },

        startTelemetry: (queries) => {
            if (!client || !browser) return;

            const state = get({ subscribe });

            // Avoid restarting if queries haven't changed
            if (JSON.stringify(queries) === JSON.stringify(state.activeQueries) && telemetryAbort) return;

            // Cancel previous telemetry stream
            if (telemetryAbort) telemetryAbort.abort();
            telemetryAbort = new AbortController();
            const signal = telemetryAbort.signal;

            const request = {
                sessionIdentifier: state.sessionKey,
                sampleRateHz: 60,
                queries: queries
            };

            update(s => ({ ...s, activeQueries: queries }));

            // --- STREAM 3: Telemetry (Async Loop) ---
            (async () => {
                try {
                    // Assumed method name 'queryRealtimeTelemetry'
                    for await (const response of client.queryRealtimeTelemetry(request, { signal })) {
                        const updates = {};
                        let hasUpdates = false;

                        // Iterate over the map. In ts-proto/nice-grpc, maps are standard objects.
                        for (const [key, queryResult] of Object.entries(response.sparseQueryValues)) {
                            const queryName = queries[Number(key)];

                            if (queryResult.scalar !== undefined) {
                                updates[queryName] = queryResult.scalar;
                                hasUpdates = true;
                            } else if (queryResult.list) {
                                updates[queryName] = queryResult.list.values;
                                hasUpdates = true;
                            }
                        }

                        if (hasUpdates) {
                            update(s => ({
                                ...s,
                                telemetry: { ...s.telemetry, ...updates }
                            }));
                        }
                    }
                } catch (err) {
                    if (err.name !== 'AbortError') console.error("Telemetry error:", err);
                }
            })();
        },

        disconnect: () => {
            // Abort all streams
            if (mainSessionAbort) mainSessionAbort.abort();
            if (telemetryAbort) telemetryAbort.abort();

            mainSessionAbort = null;
            telemetryAbort = null;
            client = null;

            set(initialState);
        }
    };
}

export const sessionStore = createSessionStore();
export const connected = derived(sessionStore, $s => $s.connected);
export const drivers = derived(sessionStore, $s => $s.drivers);
export const telemetry = derived(sessionStore, $s => $s.telemetry);
export const laps = derived(sessionStore, $s => $s.laps);
export const stints = derived(sessionStore, $s => $s.stints);
export const otherCarLaps = derived(sessionStore, $s => $s.otherCarLaps);
export const otherCarStints = derived(sessionStore, $s => $s.otherCarStints);
