import { createLogger } from './logger';
const log = createLogger('metrics');

export class Counter {
    private _counts = new Map<string, number>();

    increment(name: string, labels: Record<string, string> = {}): void {
        const key = Counter._key(name, labels);
        this._counts.set(key, (this._counts.get(key) || 0) + 1);
    }

    get(name: string, labels: Record<string, string> = {}): number {
        return this._counts.get(Counter._key(name, labels)) || 0;
    }

    flush(): void {
        for (const [key, count] of this._counts) {
            const [name, labelStr] = key.split('|');
            const labels: Record<string, string> = {};
            if (labelStr) {
                for (const pair of labelStr.split(',')) {
                    const [k, v] = pair.split('=');
                    labels[k] = v;
                }
            }
            log.info({ type: 'metric', metric: name, value: count, labels }, `metric: ${name}`);
        }
        this._counts.clear();
    }

    private static _key(name: string, labels: Record<string, string>): string {
        const labelStr = Object.entries(labels)
            .sort(([a], [b]) => a.localeCompare(b))
            .map(([k, v]) => `${k}=${v}`)
            .join(',');
        return `${name}|${labelStr}`;
    }
}

export function timer(name: string): (labels?: Record<string, string>) => number {
    const start = process.hrtime.bigint();
    return function end(labels: Record<string, string> = {}): number {
        const durationMs = Number(process.hrtime.bigint() - start) / 1e6;
        log.info({ type: 'metric', metric: name, value: Math.round(durationMs), labels }, `metric: ${name}`);
        return durationMs;
    };
}

// Singleton counter with periodic flush
export const counters = new Counter();
const flushTimer = setInterval(() => counters.flush(), 60_000);
flushTimer.unref();
