// Supabase Edge Function for SCAMYNX Telemetry
// Deploy: supabase functions deploy telemetry --project-ref <YOUR_PROJECT_REF>

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface DeviceInfo {
  manufacturer?: string
  model?: string
  android_version?: string
  app_version?: string
}

interface TelemetryEvent {
  session_id: string
  event_type: string
  timestamp: string
  payload?: Record<string, string>
  app_version?: string
  device_info?: DeviceInfo
}

interface BatchRequest {
  events: TelemetryEvent[]
  batch_id: string
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    const url = new URL(req.url)
    const path = url.pathname.split('/').pop()

    // POST /v1/telemetry/event - Single event
    if (path === 'event' && req.method === 'POST') {
      const event: TelemetryEvent = await req.json()
      
      const { error } = await supabase
        .from('telemetry_events')
        .insert({
          session_id: event.session_id,
          event_type: event.event_type,
          timestamp: event.timestamp,
          payload: event.payload ?? {},
          app_version: event.app_version ?? event.device_info?.app_version,
          device_manufacturer: event.device_info?.manufacturer,
          device_model: event.device_info?.model,
          android_version: event.device_info?.android_version,
        })

      if (error) throw error

      return new Response(
        JSON.stringify({ status: 'ok', processed_events: 1 }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // POST /v1/telemetry/batch - Batch events
    if (path === 'batch' && req.method === 'POST') {
      const batch: BatchRequest = await req.json()
      
      const records = batch.events.map(event => ({
        session_id: event.session_id,
        event_type: event.event_type,
        timestamp: event.timestamp,
        payload: event.payload ?? {},
        app_version: event.app_version ?? event.device_info?.app_version,
        device_manufacturer: event.device_info?.manufacturer,
        device_model: event.device_info?.model,
        android_version: event.device_info?.android_version,
        batch_id: batch.batch_id,
      }))

      const { error } = await supabase
        .from('telemetry_events')
        .insert(records)

      if (error) throw error

      return new Response(
        JSON.stringify({ 
          status: 'ok', 
          processed_events: batch.events.length,
          batch_id: batch.batch_id 
        }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    return new Response(
      JSON.stringify({ status: 'error', message: 'Invalid endpoint' }),
      { status: 404, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    console.error('Telemetry error:', error)
    return new Response(
      JSON.stringify({ status: 'error', message: error.message }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})
