import com.google.gdata.data.PlainTextConstruct
import com.google.gdata.data.calendar.CalendarEntry
import com.google.gdata.util.AuthenticationException
import com.google.gdata.data.calendar.CalendarFeed
import com.google.gdata.client.calendar.CalendarService
import com.google.gdata.data.calendar.CalendarEventEntry
import com.google.gdata.data.calendar.CalendarEventFeed
import com.google.gdata.client.calendar.CalendarQuery
import com.google.gdata.data.batch.BatchOperationType
import com.google.gdata.data.batch.BatchUtils
import com.google.gdata.data.Link
import com.google.gdata.data.ILink
import com.google.gdata.data.DateTime;

import com.google.gdata.data.acl.AclEntry
import com.google.gdata.data.acl.AclScope
import com.google.gdata.data.calendar.CalendarAclRole

class GoogleCalendarService {

    def CALENDAR_NAME = "iceScrum"

    def getConnection(login, password) {
        CalendarService googleService = new CalendarService("iceScrum")
        try {
            googleService.setUserCredentials(login, password);
        }
        catch (AuthenticationException e) {
            throw new RuntimeException('is.googleAgenda.error.wrongCredentials')
        }
        return googleService
    }




    def createCalendar(CalendarService service, login, calendarName) {
        CalendarEntry calendar = getCalendar(service, login, calendarName)
        while(calendar == null) {
            calendar = new CalendarEntry()
            calendar.setTitle(new PlainTextConstruct(calendarName))
            calendar.setSummary(new PlainTextConstruct("Auto-generated by iceScrum"))
            calendar = postCalendar(service, login, calendar)
            setCalendarToPublic(service, getCalendar(service, login, calendarName), login)

            if(getCalendar(service, login, calendarName) == null) {
				try {
					calendar.delete()
					calendar = null
				} catch (Exception e) {
					if(log.debugEnabled) e.printStackTrace()
				}
			}
        }
		return calendar;
    }

    def postCalendar(CalendarService service, login, CalendarEntry calendar) {
        try {
            return service.insert(getPostUrl(login, true), calendar);
        }
        catch (Exception e) {
            if(log.debugEnabled) e.printStackTrace()
        }
        return calendar;
    }

    def getCalendar(CalendarService service, login, calendarName) {
        try {
            CalendarFeed resultFeed = service?.getFeed(getPostUrl(login, true), CalendarFeed.class)
            CalendarEntry calendar = null
            for(int i = 0; i < resultFeed?.getEntries()?.size() && calendar == null; i++) {
                if(resultFeed.getEntries().get(i)?.getTitle()?.getPlainText()?.equals(calendarName)) {
                    calendar = resultFeed.getEntries().get(i)
                }
            }
            if(calendar == null) {
            }
            return calendar;
        } catch (Exception e) {
            if (log.debugEnabled) e.printStackTrace()
        }
    }

    def deleteCalendar(CalendarService service, String login, String calendarName) {
        CalendarEntry calendar = getCalendar(service, login, calendarName)
        try {
            calendar.delete()
        } catch (Exception e) {
            System.out.println("Unable to delete calendar : "+calendar.getTitle().getPlainText())
        }
    }

    def getPostUrl(login, isCalendar) {
        if(isCalendar)
            return new URL("https://www.google.com/calendar/feeds/"+login+"/owncalendars/full")
        return new URL("https://www.google.com/calendar/feeds/"+login+"/private/full")
    }

    def getNewEvent(eventName, comment) {
        CalendarEventEntry newEvent = new CalendarEventEntry()
        newEvent.setTitle(new PlainTextConstruct(eventName))
        if(comment)
            newEvent.setContent(new PlainTextConstruct(comment))
        return newEvent
    }

    def sendEvent(CalendarService service, login, event){
        CalendarEntry cal = getCalendar(service, login, CALENDAR_NAME)
        if(cal)
            return service?.insert(new URL(getCalendarURL(cal, login)), event)
        return null
    }

    def getCalendarURL(CalendarEntry c, login) {
		return c?.getId()?.replace("%40", "@")?.replace(login+"/calendars/", "")+"/private/full"
	}

    def getCalendarPublicURL(service, login, calendarName) {
		return "http://www.google.com/calendar/embed?src=" + getCalendarID(getCalendar(service, login, calendarName), login)
	}

    def getCalendarID(CalendarEntry c, login) {
		String s = getCalendarURL(c, login).replace("http://www.google.com/calendar/feeds/", "")
		return s.substring(0, s.indexOf('/'))
	}

    def setCalendarToPublic(CalendarService service, CalendarEntry c, login) {
		AclEntry entry = new AclEntry()
		entry.setScope(new AclScope(AclScope.Type.DEFAULT, null))
		entry.setRole(CalendarAclRole.READ)
		try {
			URL aclUrl = new URL("https://www.google.com/calendar/feeds/"+ getCalendarID(c, login) +"/acl/full")
			AclEntry insertedEntry = service.insert(aclUrl, entry)
		}
        catch (Exception e) {
			if (log.debugEnabled) e.printStackTrace()
		}
	}

    def emptyCalendar(CalendarService service, CalendarEntry c, String login, String startTime) {
        try {
			URL feedUrl = new URL(getCalendarURL(c, login));
            CalendarQuery query = new CalendarQuery(feedUrl);
			query.setMaxResults(10000)
            if(startTime != null) query.setMinimumStartTime(DateTime.parseDateTime(startTime))
			CalendarEventFeed feed = service.query(query, CalendarEventFeed.class)
            List<CalendarEventEntry> events = feed.getEntries()
			CalendarEventFeed batchRequest = new CalendarEventFeed()
			CalendarEventEntry toDelete
			for(int i = 0; i < events.size(); i++) {
			    toDelete = events.get(i)
				BatchUtils.setBatchId(toDelete, "" + i)
				BatchUtils.setBatchOperationType(toDelete, BatchOperationType.DELETE)
				batchRequest.getEntries().add(toDelete)
            }
            Link batchLink = feed.getLink(ILink.Rel.FEED_BATCH, ILink.Type.ATOM)
			service.batch(new URL(batchLink.getHref()), batchRequest)
		} catch (Exception e) {
            if (log.debugEnabled) e.printStackTrace()
			return false
		}
		return true
	}

}