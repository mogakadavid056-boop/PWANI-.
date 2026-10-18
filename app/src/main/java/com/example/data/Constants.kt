package com.example.data

object Constants {
  // Public client credentials fallback when build-time environment secrets are not provided.
  // The anon key is a public client key; NEVER use a service_role key.
  const val DEFAULT_SUPABASE_URL = "https://zftrsxyqixnmqpcxrbyo.supabase.co"
  const val DEFAULT_SUPABASE_ANON_KEY = "sb_publishable_PzdmF_ouJmxKgmVhvYq-nQ_Jgy6o9lY"
}
