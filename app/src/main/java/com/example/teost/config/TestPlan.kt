package com.example.teost.config

import kotlinx.serialization.Serializable

@Serializable
data class TestPlan(
    val name: String,
    val description: String? = null,
    val tests: List<TestSpec>
)

@Serializable
data class TestSpec(
    val category: TestCategory,
    val type: TestType,
    val target: Target,
    val params: Params = Params(),
    val enabled: Boolean = true
)

@Serializable
data class Target(
    val targetUrl: String? = null,
    val host: String? = null,
    val portList: List<Int>? = null,
    val endpointList: List<String>? = null
)

@Serializable
enum class TestCategory {
    DDOS_PROTECTION,
    WEB_PROTECTION,
    BOT_MANAGEMENT,
    API_PROTECTION
}

@Serializable
enum class TestType {
    // DoS / Network Protection
    HTTP_SPIKE,
    IP_REGION_BLOCKING,
    TCP_PORT_REACHABILITY,
    UDP_REACHABILITY,
    CONNECTION_FLOOD,

    // Web Protection (WAF & Rules)
    SQLI_XSS_SMOKE,
    REFLECTED_XSS,
    PATH_TRAVERSAL_INJECTION_LOG4SHELL,
    CUSTOM_RULES,
    EDGE_RATE_LIMITING,
    OVERSIZED_PAYLOAD,

    // Bot Management
    USER_AGENT_ANOMALY,
    COOKIE_JS_CHALLENGE,
    WEB_CRAWLER_SIM,
    CLIENT_REPUTATION,

    // API Protection
    CONTEXT_AWARE_RATE_LIMIT,
    AUTHENTICATION_TEST,
    BRUTE_FORCE,
    ENUMERATION_IDOR,
    SCHEMA_INPUT_VALIDATION,
    BUSINESS_LOGIC_ABUSE
}

@Serializable
data class Params(
    // HTTP Spike / CC Defense
    val burst_requests: Int? = null,
    val burst_interval_ms: Int? = null,
    val sustained_window_sec: Int? = null,
    val burst_pattern: String? = null,
    val target_url: String? = null,

    // IP/Region Blocking
    val use_vpn: Boolean? = null,
    val ip_rotation_sec: Int? = null,

    // TCP/UDP
    val timeout_ms: Int? = null,
    val port_list: List<Int>? = null,
    val udp_payload: String? = null,

    // Connection Flood
    val concurrent_connections: Int? = null,
    val connect_rate: Int? = null,

    // WAF Payload/Rules
    val payload_list: List<String>? = null,
    val encoding_mode: String? = null, // raw|urlencode|base64|case-mix
    val injection_point: String? = null, // query|body|header|path
    val target_params: List<String>? = null,
    val target_paths: List<String>? = null,
    val headers_overrides: Map<String, String>? = null,
    val method_override: String? = null, // GET|POST|PUT|DELETE...
    val rps_target: Int? = null,
    val window_sec: Int? = null,
    val fingerprint_mode: String? = null,
    val param_length: Int? = null,
    val body_size_kb: Int? = null,
    val field_repeats: Int? = null,

    // Bot Management
    val ua_profiles: List<String>? = null,
    val rotate_mode: String? = null, // sequential|random
    val cookie_policy: String? = null, // disabled|enabled
    val js_exec_mode: String? = null,
    val crawl_depth: Int? = null,
    val humanization: Boolean? = null,

    // API Protection
    val token_list: List<String>? = null,
    val request_pattern: String? = null, // burst|steady
    val parallel_users: Int? = null,
    val endpoint_list: List<String>? = null,
    val tokens: Map<String, String>? = null, // valid|expired|malformed|missing
    val auth_header_mode: String? = null, // header|query|cookie
    val request_endpoints: List<String>? = null,
    val username: String? = null,
    val password_list: List<String>? = null,
    val attempts_per_minute: Int? = null,
    val concurrency: Int? = null,
    val enum_template: String? = null,
    val id_range: List<Long>? = null,
    val step_size: Long? = null,
    val auth_tokens: List<String>? = null,
    val fuzz_cases: List<String>? = null,
    val oversized_field_length: Int? = null,
    val content_types: List<String>? = null,
    val special_chars: List<String>? = null,
    val replay_count: Int? = null,
    val pagination_pattern: String? = null,
    val request_delay_ms: Long? = null,
    val workflow_steps: List<WorkflowStep>? = null,
    val race_condition_test: Boolean? = null
)

@Serializable
data class WorkflowStep(
    val method: String,
    val endpoint: String,
    val headers: Map<String, String>? = null,
    val bodyTemplate: String? = null,
    val useTokenIndex: Int? = null
)


