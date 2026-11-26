export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  // Allows to automatically instantiate createClient with right options
  // instead of createClient<Database, { PostgrestVersion: 'XX' }>(URL, KEY)
  __InternalSupabase: {
    PostgrestVersion: "14.18"
  }
  public: {
    Tables: {
      closures: {
        Row: {
          checked_at: string | null
          ends_on: string
          id: string
          is_sample: boolean
          place_id: string
          reason: string | null
          review_status: Database["public"]["Enums"]["review_status"]
          source_url: string | null
          starts_on: string
        }
        Insert: {
          checked_at?: string | null
          ends_on: string
          id?: string
          is_sample?: boolean
          place_id: string
          reason?: string | null
          review_status?: Database["public"]["Enums"]["review_status"]
          source_url?: string | null
          starts_on: string
        }
        Update: {
          checked_at?: string | null
          ends_on?: string
          id?: string
          is_sample?: boolean
          place_id?: string
          reason?: string | null
          review_status?: Database["public"]["Enums"]["review_status"]
          source_url?: string | null
          starts_on?: string
        }
        Relationships: [
          {
            foreignKeyName: "closures_place_id_fkey"
            columns: ["place_id"]
            isOneToOne: false
            referencedRelation: "places"
            referencedColumns: ["id"]
          },
        ]
      }
      events: {
        Row: {
          arrival_advice_max: number | null
          arrival_advice_min: number | null
          category: Database["public"]["Enums"]["place_category"]
          checked_at: string | null
          description: string | null
          end_time: string | null
          event_date: string | null
          id: string
          is_sample: boolean
          on_site_available: boolean
          performers: string[]
          place_id: string
          price_from_idr: number | null
          recurrence: Database["public"]["Enums"]["recurrence_kind"]
          review_status: Database["public"]["Enums"]["review_status"]
          seller_url: string | null
          source_url: string | null
          start_time: string
          tags: string[]
          title: string
          weekday: number | null
        }
        Insert: {
          arrival_advice_max?: number | null
          arrival_advice_min?: number | null
          category?: Database["public"]["Enums"]["place_category"]
          checked_at?: string | null
          description?: string | null
          end_time?: string | null
          event_date?: string | null
          id?: string
          is_sample?: boolean
          on_site_available?: boolean
          performers?: string[]
          place_id: string
          price_from_idr?: number | null
          recurrence: Database["public"]["Enums"]["recurrence_kind"]
          review_status?: Database["public"]["Enums"]["review_status"]
          seller_url?: string | null
          source_url?: string | null
          start_time: string
          tags?: string[]
          title: string
          weekday?: number | null
        }
        Update: {
          arrival_advice_max?: number | null
          arrival_advice_min?: number | null
          category?: Database["public"]["Enums"]["place_category"]
          checked_at?: string | null
          description?: string | null
          end_time?: string | null
          event_date?: string | null
          id?: string
          is_sample?: boolean
          on_site_available?: boolean
          performers?: string[]
          place_id?: string
          price_from_idr?: number | null
          recurrence?: Database["public"]["Enums"]["recurrence_kind"]
          review_status?: Database["public"]["Enums"]["review_status"]
          seller_url?: string | null
          source_url?: string | null
          start_time?: string
          tags?: string[]
          title?: string
          weekday?: number | null
        }
        Relationships: [
          {
            foreignKeyName: "events_place_id_fkey"
            columns: ["place_id"]
            isOneToOne: false
            referencedRelation: "places"
            referencedColumns: ["id"]
          },
        ]
      }
      opening_hours: {
        Row: {
          checked_at: string | null
          closes_at: string | null
          id: string
          is_closed: boolean
          is_sample: boolean
          note: string | null
          opens_at: string | null
          place_id: string
          review_status: Database["public"]["Enums"]["review_status"]
          source_url: string | null
          weekday: number
        }
        Insert: {
          checked_at?: string | null
          closes_at?: string | null
          id?: string
          is_closed?: boolean
          is_sample?: boolean
          note?: string | null
          opens_at?: string | null
          place_id: string
          review_status?: Database["public"]["Enums"]["review_status"]
          source_url?: string | null
          weekday: number
        }
        Update: {
          checked_at?: string | null
          closes_at?: string | null
          id?: string
          is_closed?: boolean
          is_sample?: boolean
          note?: string | null
          opens_at?: string | null
          place_id?: string
          review_status?: Database["public"]["Enums"]["review_status"]
          source_url?: string | null
          weekday?: number
        }
        Relationships: [
          {
            foreignKeyName: "opening_hours_place_id_fkey"
            columns: ["place_id"]
            isOneToOne: false
            referencedRelation: "places"
            referencedColumns: ["id"]
          },
        ]
      }
      places: {
        Row: {
          address: string | null
          cap_code: string
          cap_radius_m: number | null
          category: Database["public"]["Enums"]["place_category"]
          cover_path: string | null
          created_at: string
          description: string | null
          id: string
          is_sample: boolean
          lat: number | null
          lng: number | null
          location: unknown
          name: string
          passport_order: number | null
          region_id: number
          slug: string
        }
        Insert: {
          address?: string | null
          cap_code: string
          cap_radius_m?: number | null
          category: Database["public"]["Enums"]["place_category"]
          cover_path?: string | null
          created_at?: string
          description?: string | null
          id?: string
          is_sample?: boolean
          lat?: number | null
          lng?: number | null
          location: unknown
          name: string
          passport_order?: number | null
          region_id: number
          slug: string
        }
        Update: {
          address?: string | null
          cap_code?: string
          cap_radius_m?: number | null
          category?: Database["public"]["Enums"]["place_category"]
          cover_path?: string | null
          created_at?: string
          description?: string | null
          id?: string
          is_sample?: boolean
          lat?: number | null
          lng?: number | null
          location?: unknown
          name?: string
          passport_order?: number | null
          region_id?: number
          slug?: string
        }
        Relationships: [
          {
            foreignKeyName: "places_region_id_fkey"
            columns: ["region_id"]
            isOneToOne: false
            referencedRelation: "regions"
            referencedColumns: ["id"]
          },
        ]
      }
      postcards: {
        Row: {
          card_style: string
          created_at: string
          id: string
          is_public: boolean
          is_sample: boolean
          message: string
          photo_path: string | null
          place_id: string
          rating: number | null
          user_id: string
        }
        Insert: {
          card_style?: string
          created_at?: string
          id?: string
          is_public?: boolean
          is_sample?: boolean
          message: string
          photo_path?: string | null
          place_id: string
          rating?: number | null
          user_id: string
        }
        Update: {
          card_style?: string
          created_at?: string
          id?: string
          is_public?: boolean
          is_sample?: boolean
          message?: string
          photo_path?: string | null
          place_id?: string
          rating?: number | null
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "postcards_place_id_fkey"
            columns: ["place_id"]
            isOneToOne: false
            referencedRelation: "places"
            referencedColumns: ["id"]
          },
        ]
      }
      profiles: {
        Row: {
          avatar_path: string | null
          created_at: string
          display_name: string | null
          home_region_id: number | null
          id: string
          is_sample: boolean
        }
        Insert: {
          avatar_path?: string | null
          created_at?: string
          display_name?: string | null
          home_region_id?: number | null
          id: string
          is_sample?: boolean
        }
        Update: {
          avatar_path?: string | null
          created_at?: string
          display_name?: string | null
          home_region_id?: number | null
          id?: string
          is_sample?: boolean
        }
        Relationships: [
          {
            foreignKeyName: "profiles_home_region_id_fkey"
            columns: ["home_region_id"]
            isOneToOne: false
            referencedRelation: "regions"
            referencedColumns: ["id"]
          },
        ]
      }
      regions: {
        Row: {
          code: string
          created_at: string
          id: number
          is_sample: boolean
          name: string
          short_name: string | null
          sort_order: number
          status: Database["public"]["Enums"]["region_status"]
        }
        Insert: {
          code: string
          created_at?: string
          id?: never
          is_sample?: boolean
          name: string
          short_name?: string | null
          sort_order?: number
          status?: Database["public"]["Enums"]["region_status"]
        }
        Update: {
          code?: string
          created_at?: string
          id?: never
          is_sample?: boolean
          name?: string
          short_name?: string | null
          sort_order?: number
          status?: Database["public"]["Enums"]["region_status"]
        }
        Relationships: []
      }
      reports: {
        Row: {
          created_at: string
          details: string | null
          id: string
          photo_path: string | null
          place_id: string | null
          postcard_id: string | null
          reason: Database["public"]["Enums"]["report_reason"]
          user_id: string
          video_id: string | null
        }
        Insert: {
          created_at?: string
          details?: string | null
          id?: string
          photo_path?: string | null
          place_id?: string | null
          postcard_id?: string | null
          reason: Database["public"]["Enums"]["report_reason"]
          user_id: string
          video_id?: string | null
        }
        Update: {
          created_at?: string
          details?: string | null
          id?: string
          photo_path?: string | null
          place_id?: string | null
          postcard_id?: string | null
          reason?: Database["public"]["Enums"]["report_reason"]
          user_id?: string
          video_id?: string | null
        }
        Relationships: [
          {
            foreignKeyName: "reports_place_id_fkey"
            columns: ["place_id"]
            isOneToOne: false
            referencedRelation: "places"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "reports_postcard_id_fkey"
            columns: ["postcard_id"]
            isOneToOne: false
            referencedRelation: "postcards"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "reports_video_id_fkey"
            columns: ["video_id"]
            isOneToOne: false
            referencedRelation: "videos"
            referencedColumns: ["id"]
          },
        ]
      }
      stamps: {
        Row: {
          id: string
          is_sample: boolean
          place_id: string
          stamped_at: string
          user_id: string
        }
        Insert: {
          id?: string
          is_sample?: boolean
          place_id: string
          stamped_at?: string
          user_id: string
        }
        Update: {
          id?: string
          is_sample?: boolean
          place_id?: string
          stamped_at?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "stamps_place_id_fkey"
            columns: ["place_id"]
            isOneToOne: false
            referencedRelation: "places"
            referencedColumns: ["id"]
          },
        ]
      }
      subscriptions: {
        Row: {
          category: Database["public"]["Enums"]["place_category"] | null
          created_at: string
          id: string
          place_id: string | null
          user_id: string
        }
        Insert: {
          category?: Database["public"]["Enums"]["place_category"] | null
          created_at?: string
          id?: string
          place_id?: string | null
          user_id: string
        }
        Update: {
          category?: Database["public"]["Enums"]["place_category"] | null
          created_at?: string
          id?: string
          place_id?: string | null
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "subscriptions_place_id_fkey"
            columns: ["place_id"]
            isOneToOne: false
            referencedRelation: "places"
            referencedColumns: ["id"]
          },
        ]
      }
      ticket_prices: {
        Row: {
          checked_at: string | null
          id: string
          is_sample: boolean
          label: string
          on_site_available: boolean
          place_id: string
          price_idr: number | null
          review_status: Database["public"]["Enums"]["review_status"]
          seller_url: string | null
          source_url: string | null
        }
        Insert: {
          checked_at?: string | null
          id?: string
          is_sample?: boolean
          label: string
          on_site_available?: boolean
          place_id: string
          price_idr?: number | null
          review_status?: Database["public"]["Enums"]["review_status"]
          seller_url?: string | null
          source_url?: string | null
        }
        Update: {
          checked_at?: string | null
          id?: string
          is_sample?: boolean
          label?: string
          on_site_available?: boolean
          place_id?: string
          price_idr?: number | null
          review_status?: Database["public"]["Enums"]["review_status"]
          seller_url?: string | null
          source_url?: string | null
        }
        Relationships: [
          {
            foreignKeyName: "ticket_prices_place_id_fkey"
            columns: ["place_id"]
            isOneToOne: false
            referencedRelation: "places"
            referencedColumns: ["id"]
          },
        ]
      }
      videos: {
        Row: {
          caption: string | null
          created_at: string
          duration_s: number | null
          id: string
          is_sample: boolean
          place_id: string
          stream_uid: string | null
          thumbnail_path: string | null
          user_id: string
        }
        Insert: {
          caption?: string | null
          created_at?: string
          duration_s?: number | null
          id?: string
          is_sample?: boolean
          place_id: string
          stream_uid?: string | null
          thumbnail_path?: string | null
          user_id: string
        }
        Update: {
          caption?: string | null
          created_at?: string
          duration_s?: number | null
          id?: string
          is_sample?: boolean
          place_id?: string
          stream_uid?: string | null
          thumbnail_path?: string | null
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "videos_place_id_fkey"
            columns: ["place_id"]
            isOneToOne: false
            referencedRelation: "places"
            referencedColumns: ["id"]
          },
        ]
      }
    }
    Views: {
      [_ in never]: never
    }
    Functions: {
      place_facts: { Args: { p_place_id: string }; Returns: Json }
      release_check: { Args: never; Returns: undefined }
      stamp_distance: {
        Args: { p_lat: number; p_lng: number; p_place_id: string }
        Returns: {
          cap_radius_m: number
          distance_m: number
          within_radius: boolean
        }[]
      }
    }
    Enums: {
      place_category:
        | "museum"
        | "candi"
        | "tari"
        | "pentas"
        | "seni_musik"
        | "kerajinan"
        | "lainnya"
      recurrence_kind: "once" | "weekly" | "daily"
      region_status: "available" | "upcoming" | "not_open"
      report_reason:
        | "hours_or_price_changed"
        | "place_closed"
        | "ceremony_shown_without_permission"
        | "inappropriate_media"
        | "other"
      review_status: "pending" | "published"
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, "public">]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends (DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never) = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends (PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never) = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  public: {
    Enums: {
      place_category: [
        "museum",
        "candi",
        "tari",
        "pentas",
        "seni_musik",
        "kerajinan",
        "lainnya",
      ],
      recurrence_kind: ["once", "weekly", "daily"],
      region_status: ["available", "upcoming", "not_open"],
      report_reason: [
        "hours_or_price_changed",
        "place_closed",
        "ceremony_shown_without_permission",
        "inappropriate_media",
        "other",
      ],
      review_status: ["pending", "published"],
    },
  },
} as const
