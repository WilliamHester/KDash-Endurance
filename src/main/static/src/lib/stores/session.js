import { writable, get, derived } from 'svelte/store';
import { browser } from '$app/environment';
import { LiveTelemetryServiceDefinition } from '$lib/grpc/live_telemetry_service';
import { createChannel, createClient } from 'nice-grpc-web';

const initialState = {
    connected: false,
    sessionKey: null,
    drivers: new Map(),
    staticSessionInfo: null,
    laps: [],
    stints: [],
    otherCarLaps: [],
    otherCarStints: [],
    telemetry: {},
    activeQueries: []
};

function upsert(list, item, matchFn) {
    const idx = list.findIndex(matchFn);
    if (idx !== -1) {
        const copy = [...list];
        copy[idx] = item;
        return copy;
    }
    return [item, ...list];
}

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
                            if (response.driverLap) {
                                newState.laps = upsert(state.laps, response.driverLap, l => l.lapNum === response.driverLap.lapNum);
                            }
                            if (response.driverStint) {
                                newState.stints = upsert(state.stints, response.driverStint, s => s.outLap === response.driverStint.outLap);
                            }
                            if (response.otherCarLap) {
                                newState.otherCarLaps = upsert(state.otherCarLaps, response.otherCarLap, l => l.carId === response.otherCarLap.carId && l.lapNum === response.otherCarLap.lapNum);
                            }
                            if (response.otherCarStint) {
                                newState.otherCarStints = upsert(state.otherCarStints, response.otherCarStint, s => s.carIdx === response.otherCarStint.carIdx && s.outLap === response.otherCarStint.outLap);
                            }
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

            // --- CALL: Static Session Info ---
            (async () => {
                try {
                    const response = await client.getStaticSessionInfo(request, { signal });
                    update(state => ({ ...state, staticSessionInfo: response }));
                } catch (err) {
                    if (err.name !== 'AbortError') console.error("Static Session Info error:", err);
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
export const driversList = derived(sessionStore, $s => Array.from($s.drivers.values()));
export const staticSessionInfo = derived(sessionStore, $s => $s.staticSessionInfo);
export const telemetry = derived(sessionStore, $s => $s.telemetry);
export const laps = derived(sessionStore, $s => $s.laps);
export const stints = derived(sessionStore, $s => $s.stints);
export const otherCarLaps = derived(sessionStore, $s => $s.otherCarLaps);
export const otherCarStints = derived(sessionStore, $s => $s.otherCarStints);
