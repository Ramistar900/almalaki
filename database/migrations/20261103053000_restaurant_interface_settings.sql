create table public.restaurant_interface_settings (
    restaurant_id uuid primary key
        references public.restaurants(id) on delete cascade,
    news_text text not null default 'أهلاً بكم في Royal Coffee ☕'
        check (char_length(news_text) <= 240),
    active_logo_id text not null default 'default_royal'
        check (char_length(active_logo_id) between 1 and 100),
    active_logo_name text not null default 'الشعار الأساسي'
        check (char_length(active_logo_name) <= 100),
    active_logo_path text,
    active_logo_animation text not null default 'layered'
        check (active_logo_animation in ('layered', 'generic_3d')),
    text_logo_text text not null default 'Royal Coffee'
        check (char_length(text_logo_text) between 1 and 120),
    text_logo_style text not null default 'MODERN_SIGNATURE'
        check (text_logo_style in ('CLASSIC_CALLIGRAPHY', 'MODERN_SIGNATURE')),
    text_logo_font_size integer not null default 27
        check (text_logo_font_size between 16 and 56),
    updated_at timestamptz not null default now(),
    updated_by uuid references auth.users(id),
    constraint restaurant_interface_logo_path_tenant_check
        check (
            active_logo_path is null
            or active_logo_path like restaurant_id::text || '/branding/%'
        )
);

alter table public.restaurant_interface_settings enable row level security;

revoke all on table public.restaurant_interface_settings from public;
grant select on table public.restaurant_interface_settings to anon, authenticated;
grant insert, update on table public.restaurant_interface_settings to authenticated;

create policy "Public can read active restaurant interface settings"
on public.restaurant_interface_settings
for select
to anon
using (private.is_active_restaurant(restaurant_id));

create policy "Authenticated users can read active restaurant interface settings"
on public.restaurant_interface_settings
for select
to authenticated
using (private.is_active_restaurant(restaurant_id));

create policy "Restaurant owners can create their interface settings"
on public.restaurant_interface_settings
for insert
to authenticated
with check (
    private.is_restaurant_owner(restaurant_id)
    and (updated_by is null or updated_by = (select auth.uid()))
);

create policy "Restaurant owners can update their interface settings"
on public.restaurant_interface_settings
for update
to authenticated
using (private.is_restaurant_owner(restaurant_id))
with check (
    private.is_restaurant_owner(restaurant_id)
    and (updated_by is null or updated_by = (select auth.uid()))
);

alter publication supabase_realtime
add table public.restaurant_interface_settings;
