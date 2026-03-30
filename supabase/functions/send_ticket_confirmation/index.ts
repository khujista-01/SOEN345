import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { userEmail, userName, eventTitle, eventDate, eventLocation, ticketId, ticketPrice } = await req.json()
    const recipientEmail = String(userEmail ?? '').trim().toLowerCase()

    if (!recipientEmail || !eventTitle) {
      return new Response(
        JSON.stringify({ error: 'Missing required fields' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    const resendApiKey = Deno.env.get('RESEND_API_KEY')
    if (!resendApiKey) throw new Error('RESEND_API_KEY not configured')

    const resendFromEmail = Deno.env.get('RESEND_FROM_EMAIL')?.trim() || 'onboarding@resend.dev'
    const resendFromName = Deno.env.get('RESEND_FROM_NAME')?.trim() || 'Ticket Reservation'
    const fromAddress = `${resendFromName} <${resendFromEmail}>`
    const subject = `Ticket confirmed for ${eventTitle}`

    const html = `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h1 style="color: #2563eb;">🎫 Ticket Confirmed!</h1>
        <p>Hi <strong>${userName ?? 'User'}</strong>,</p>
        <p>Your ticket has been reserved! 🎉</p>
        <div style="background: #f8fafc; padding: 20px; border-radius: 8px; margin: 20px 0;">
          <h2>Event Details</h2>
          <p><strong>Event:</strong> ${eventTitle}</p>
          <p><strong>Date:</strong> ${eventDate}</p>
          <p><strong>Location:</strong> ${eventLocation}</p>
          <p><strong>Ticket ID:</strong> ${ticketId}</p>
          <p><strong>Price:</strong> $${ticketPrice}</p>
        </div>
        <p>Show this email at the venue. See you there!</p>
        <p>Best,<br>Your Events Team</p>
      </div>
    `

    const res = await fetch('https://api.resend.com/emails', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${resendApiKey}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        from: fromAddress,
        to: [recipientEmail],
        subject,
        html,
      }),
    })

    const resendBody = await res.text()
    console.log(
      `send_ticket_confirmation recipient=${recipientEmail} from=${fromAddress} subject="${subject}" status=${res.status} body=${resendBody}`
    )

    if (!res.ok) {
      return new Response(JSON.stringify({ error: `Email failed: ${resendBody}` }), { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    return new Response(
      JSON.stringify({ success: true, recipientEmail, fromAddress, resendResponse: resendBody }),
      { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    return new Response(JSON.stringify({ error: message }), { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
  }
})
