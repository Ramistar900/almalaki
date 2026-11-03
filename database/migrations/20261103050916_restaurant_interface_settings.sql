create table public.restaurant_interface_settings (
 restaurant_id uuid primary key references public.restaurants(id) on delete cascade,
 news_text text not null default '',
 active_logo_id text not null default 'default_royal',
 active_logo_name text not null default 'الشعار الأساسي',
 active_logo_path text,
 active_logo_animation text not null default 'layered',
 text_logo_text text not null default 'Royal Coffee',
 text_logo_style text not null default 'MODERN_SIGNATURE',
 text_logo_font_size integer not null default 27,
 updated_at timestamptz not null default now(),
 updated_by uuid references auth.users(id),
 constraint restaurant_interface_logo_path_tenant_check check (
  active_logo_path is null or active_logo_path like restaurant_id::text || '/branding/%'
 ),
 constraint restaurant_interface_text_style_check check (text_logo_style in ('CLASSIC_CALLIGRAPHY','MODERN_SIGNATURE')),
 constraint restaurant_interface_font_size_check check (text_logo_font_size between 16 and 56),
 constraint restaurant_interface_animation_check check (active_logo_animation in ('layered','generic_3d'))
);
alter table public.restaurant_interface_settings enable row level security;
grant select on public.restaurant_interface_settings to anon, authenticated;
grant insert, update on public.restaurant_interface_settings to authenticated;
create policy restaurant_interface_public_read on public.restaurant_interface_settings for select to anon using (private.is_active_restaurant(restaurant_id));
create policy restaurant_interface_authenticated_read on public.restaurant_interface_settings for select to authenticated using (private.is_active_restaurant(restaurant_id));
create policy restaurant_interface_owner_insert on public.restaurant_interface_settings for insert to authenticated with check (private.is_restaurant_owner(restaurant_id));
create policy restaurant_interface_owner_update on public.restaurant_interface_settings for update to authenticated using (private.is_restaurant_owner(restaurant_id)) with check (private.is_restaurant_owner(restaurant_id));
alter publication supabase_realtime add table public.restaurant_interface_settings;