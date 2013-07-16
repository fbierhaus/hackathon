package com.verizon.mms.model;

import com.nbi.common.NBIException;
import com.nbi.location.LocationException;
import com.nbi.map.route.RouteErrorCode;

/*
 * Contains mapping from NBI error code to error messages that can be displayed to user
 */
public class ErrorInformation {

    //Returns end-user error message string by specified NBI error code
    public static String getErrorMessage(int errorCode) {

        String errorMessage;

        switch(errorCode) {

        case NBIException.NBI_NETWORK_ERROR2001:
        case NBIException.NBI_NETWORK_ERROR2002:
        case NBIException.NBI_NETWORK_ERROR2003:

        case RouteErrorCode.NBI_ROUTE_ERROR3001:
        case RouteErrorCode.NBI_ROUTE_ERROR3006:
        case RouteErrorCode.NBI_ROUTE_ERROR3007:
        case RouteErrorCode.NBI_ROUTE_ERROR3008:
        case RouteErrorCode.NBI_ROUTE_ERROR3009:
            errorMessage = "Network error";
            break;

        case NBIException.NBI_NETWORK_WRITE_ERROR:
            errorMessage = "Failed to write to a network socket";
            break;

        case NBIException.NBI_NETWORK_READ_ERROR:
            errorMessage = "Failed to read from a network socket";
            break;

        case NBIException.NBI_NETWORK_ERROR2004:
            errorMessage = "An error occurred trying to connect to the server. Error code:";
            break;

        case RouteErrorCode.NBI_ROUTE_BAD_DESTINATION:
            errorMessage = "Routing destination is not near a routable road";
            break;

        case RouteErrorCode.NBI_ROUTE_BAD_ORIGIN:
            errorMessage = "Origin no near a routable road";
            break;

        case RouteErrorCode.NBI_ROUTE_CANNOT_ROUTE:
            errorMessage = "No route can be found between the origin and the destination";
            break;

        case NBIException.NBI_SERVER_ERROR4000:
        case NBIException.NBI_SERVER_ERROR4001:
        case NBIException.NBI_SERVER_ERROR4002:
        case NBIException.NBI_SERVER_ERROR4003:
        case NBIException.NBI_SERVER_ERROR4010:
        case NBIException.NBI_SERVER_ERROR4014:
        case NBIException.NBI_SERVER_ERROR4015:
        case NBIException.NBI_SERVER_ERROR4016:
        case NBIException.NBI_SERVER_ERROR4017:
        case NBIException.NBI_SERVER_ERROR4020:
        case NBIException.NBI_SERVER_ERROR4021:
        case NBIException.NBI_SERVER_ERROR4022:
        case NBIException.NBI_SERVER_ERROR4030:
        case NBIException.NBI_SERVER_ERROR4031:
            errorMessage = "Server error";
            break;
            
        case NBIException.NBI_SERVER_ERROR_QUERY_LIMIT_REACHED:
            errorMessage = "The maximum daily queries limit for this API Key has been reached. Please try again tomorrow.";
        break;
        
        case NBIException.NBI_SERVER_ERROR_MDN_LIMIT_REACHED:
            errorMessage = "The maximum number of users for this API Key has been reached. Additional users will be rejected.";
        break;

        case NBIException.NBI_SERVER_FEATURE_NOT_AVAILABLE:
            errorMessage = "Feature not available in this country.";
            break;
            
        case NBIException.NBI_SERVER_FEATURE_NOT_SUPPORTED:
            errorMessage = "Feature not supported.";
            break;
            
        case NBIException.NBI_SERVER_ERROR_INVALID_API_KEY:
            errorMessage = "The specified API Key is not valid. Please visit developer.verizon.com to obtain a valid API Key.";
        break;
        
        case LocationException.NBI_ERROR_NO_LOCATION_AVAILABLE:
            errorMessage = "Location Kit failed to get a location from the server";
            break;
            
        case LocationException.NBI_ERROR_GPS_TIMEOUT:
            errorMessage = "Location timed out.";
            break;
            
        case LocationException.NBI_NETWORK_ERROR2000:
            errorMessage = "GPS General failure";
            break;
            
        case LocationException.NBI_ERROR_GPS_TURNED_OFF:
            errorMessage = "GPS turned off.";
            break;
            
        case LocationException.NBI_ERROR_NETWORK_TIMEOUT:
            errorMessage = "Location Kit network timed out.";
        break;
        
        default:
            errorMessage = "Internal error";
            break;
        }

        errorMessage += " (" + errorCode + ")";
        return errorMessage;
    }
}