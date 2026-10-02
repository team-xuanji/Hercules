package team.magic.flute.hercules.twelve.labors.auth;

/**
 * User Login Information Annotation
 *
 * <p>This annotation marks controller method parameters that should be automatically
 * populated with user authentication and authorization information by the
 * {@link UserInfoResolver}. It serves as a declarative way to inject user context
 * into business operation endpoints within the Hercules business access gateway.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * this annotation enables seamless user context injection, supporting secure access
 * control and audit logging across various business functions including data export,
 * document processing, and other business operations.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * @PostMapping("/export-data")
 * public ResponseEntity<?> exportData(
 *     @UserLogInInfo UserAccessInfo userInfo,
 *     @RequestBody ExportRequest request) {
 *     // userInfo is automatically populated with authentication context
 *     return exportService.performExport(request, userInfo);
 * }
 * }</pre>
 *
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>Automatic user authentication context injection</li>
 *   <li>Support for multi-tenant access control</li>
 *   <li>Integration with various authentication systems</li>
 *   <li>Audit logging and compliance support</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> This annotation works in conjunction with
 * {@link UserInfoResolver} to provide seamless authentication context injection
 * throughout the business access gateway, ensuring that all business operations
 * have access to proper user identity and authorization information.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 * @see UserAccessInfo
 * @see UserInfoResolver
 */
public @interface UserLogInInfo {
}
