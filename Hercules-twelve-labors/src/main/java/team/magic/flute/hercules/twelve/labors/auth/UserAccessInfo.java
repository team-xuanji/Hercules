package team.magic.flute.hercules.twelve.labors.auth;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * User Access Information Container
 *
 * <p>This class encapsulates user authentication and authorization information for the
 * Hercules business access gateway. It serves as a central container for user identity
 * and access control data, enabling secure access to business operations and resources
 * within the Hercules ecosystem.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * this class supports multi-tenant access control, ensuring that business operations are
 * properly authorized and isolated between different applications and users.
 *
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>Application-level authentication through appKey</li>
 *   <li>User-level identification and authorization</li>
 *   <li>Support for role-based access control</li>
 *   <li>Integration with external authentication systems</li>
 * </ul>
 *
 * <p><strong>Usage:</strong> This class is typically populated by authentication resolvers
 * and used throughout the business access gateway to enforce security policies and
 * access controls for various business functions.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class UserAccessInfo {

    /**
     * Application key for application-level authentication
     *
     * <p>This key identifies the client application accessing the business gateway
     * and is used for application-level authorization and resource isolation.
     */
    private String appKey;

    /**
     * Unique user identifier
     *
     * <p>This identifier uniquely identifies the user within the system and is used
     * for user-level authorization, audit logging, and resource ownership tracking.
     */
    private String userId;

    /**
     * Human-readable user name
     *
     * <p>This name is used for display purposes and audit logging, providing a
     * human-friendly identifier for the user in logs and user interfaces.
     */
    private String userName;

    /**
     * Get the user display name
     *
     * <p>This method provides a convenient way to retrieve the user's display name
     * for logging, auditing, and user interface purposes.
     *
     * @return the user's display name
     */
    public String getUser(){
        return userName;
    }
}
