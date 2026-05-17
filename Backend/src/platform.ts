/**
 * Platform discriminator helpers (PORTING_CRITERIA.md T1.9.a, T1.10.b).
 *
 * Clients identify their platform via the `X-App-Platform` HTTP header. The iOS
 * client has been sending this header since the App Attest era (`Services.swift`
 * `addValue("ios", forHTTPHeaderField: "X-App-Platform")`) — historically as
 * observability metadata, now also used for dispatch. Reusing the existing iOS
 * header means **zero iOS changes** are required for the Android port.
 *
 * Values:
 *   - "ios"      — App Attest path (default when header is absent or unrecognized;
 *                  preserves backward-compatibility with in-field iOS builds that
 *                  predate the header).
 *   - "android"  — Play Integrity path.
 *
 * The default-to-ios behavior is load-bearing for T1.10.b: real iOS builds in
 * users' phones that pre-date the header must continue to work indistinguishably
 * from before.
 */
export type Platform = 'ios' | 'android';

export const PLATFORM_HEADER = 'x-app-platform';

export function resolvePlatform(headerValue: string | string[] | undefined): Platform {
    if (typeof headerValue !== 'string') return 'ios';
    const v = headerValue.trim().toLowerCase();
    return v === 'android' ? 'android' : 'ios';
}
